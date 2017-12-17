package chenwt.pku.edu.cn.ssdormselect;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DormSelectActivity extends AppCompatActivity implements View.OnClickListener{

    private SharedPreferences sp; //实例化SharedPreference对象，用于取出所存用户名

    private static final int UPDATE_ROOM_NUMBER = 1;

    private String genderNo = "1", gender;

    private int selectNum = 1;

    private Context context = this;

    private TextView studentidTv, nameTv, genderTv, vccodeTv;
    private RadioGroup dormNoRadioGroup;
    private RadioButton dorm5RadioButton, dorm13RadioButton, dorm14RadioButton, dorm8RadioButton, dorm9RadioButton;
    private ImageView mDormUpdateBtn, mBackIndividualBtn;
    private Button mConfirmBtn;
    private ClearEditText mate1IdET, mate1CodeET, mate2IdET, mate2CodeET, mate3IdET, mate3CodeET;
    private FloatingActionButton mBackLoginFab;

    private OkHttpClient client;

    private Handler mHandler = new Handler(){   //线程间消息处理机制，MessageQueue是一个存放消息对象的队列
        public void handleMessage(Message msg){
            switch (msg.what){      //消息对象的属性.what只能放数字，用作判断是哪个非主线程传来的消息
                case UPDATE_ROOM_NUMBER:
                    updateDormNumber((UserInfo) msg.obj); //在主线程中更新控件信息
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dorm_select);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //获得sp实例对象
        sp = this.getSharedPreferences("userInfo", Context.MODE_WORLD_READABLE);

        initViews();        //初始化控件显示

        client = new OkHttpClient.Builder()
                .connectTimeout(2, TimeUnit.SECONDS)        //设置连接超时时间为2s
/*                .writeTimeout(10, TimeUnit.SECONDS)         //设置读取的超时时间
                .readTimeout(30, TimeUnit.SECONDS)          //设置写入的超时时间*/
                .sslSocketFactory(TrustAllCerts.createSSLSocketFactory())
                .hostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }
                })
                .build();

        gender = sp.getString("GENDER", "男");
        if (gender.equals("男"))
            genderNo = "1";
        else if (gender.equals("女"))
            genderNo = "2";
        getRoom(genderNo);

        mBackIndividualBtn.setOnClickListener(this);
        mDormUpdateBtn.setOnClickListener(this);
        mConfirmBtn.setOnClickListener(this);
        mBackLoginFab.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.dorm_select_update_btn){    //如果点击到刷新，获取剩余空床数信息
            getRoom(genderNo);
            Toast.makeText(DormSelectActivity.this, "各宿舍楼剩余空床数信息更新成功！", Toast.LENGTH_SHORT).show();
        }
        if (v.getId() == R.id.dorm_select_back_individual){   //如果点击到返回个人信息界面，则弹出个人信息界面到前端
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        }
        if (v.getId() == R.id.fab){
            SharedPreferences.Editor editor = sp.edit();
            editor.putBoolean("isREMEMBER",false);
            editor.putBoolean("isLOAD",false);
            editor.commit();
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
        if (v.getId() == R.id.dorm_select_next_btn){   //如果点击到提交住宿办理信息，则弹出后续的办理成功或失败界面到前端
            if (saveAndPostSelectRoom()){
                /**
                 * 要是想在非主线程使用Toast，方法一：因为除了Activity ui线程默认创建之外，
                 * 其他线程不会自动创建调用 Looper来给线程创建消息循环，那就自己创建一个[Looper.prepare()、Looper.loop()]
                 */
                new Thread(){
                    @Override
                    public void run() {
                        Looper.prepare();   //通过Looper.prepare()来给线程创建消息循环。
                        try {
                            Thread.sleep(500);
                            SharedPreferences.Editor editor = sp.edit();
                            if (sp.getBoolean("SUCCESS", false)){
                                Toast.makeText(context, selectNum + "人办理，成功入住"
                                        + sp.getString("BUILDINGNO", "<还未选择>") + "号楼！", Toast.LENGTH_SHORT).show();
                                editor.putString("BUILDINGNO", "");
                                Intent intent = new Intent(context, FinishActivity.class);
                                startActivity(intent);
                                finish();
                            }
                            else {
                                Toast.makeText(context, selectNum +
                                        "人办理，办理失败！\n请检查网络及办理信息！", Toast.LENGTH_SHORT).show();
                            }
                            selectNum = 1;
                            editor.putBoolean("SUCCESS", false);
                            editor.apply();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Looper.loop();  //然后再通过Looper.loop()来使消息循环起作用。
                    }
                }.start();
                /**
                 * 要是想在非主线程使用Toast，方法二：Toast的代码创建在Runnable中，然后在需要Toast时，
                 * 把这个Runnable对象传给runOnUiThread(Runnable)。
                 * 这样Runnable对像就能在ui程序中被调用。如果当前线程是UI线程,那么行动是立即执行
                 */
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        try {
//                            Thread.sleep(500);
//                            if (sp.getBoolean("SUCCESS", false)){
//                                Toast.makeText(context, selectNum + "人办理，成功入住"
//                                        + sp.getString("BUILDINGNO", "<还未选择>") + "号楼！", Toast.LENGTH_SHORT).show();
//                                Intent intent = new Intent(context, MainActivity.class);
//                                startActivity(intent);
//                                finish();
//                            }
//                            else {
//                                Toast.makeText(context, selectNum +
//                                        "人办理，办理失败！\n请检查网络及办理信息！", Toast.LENGTH_SHORT).show();
//                            }
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                });
            }
            else {
                Toast.makeText(DormSelectActivity.this, selectNum + "人办理，还没选择住哪儿呢！", Toast.LENGTH_SHORT).show();
                selectNum = 1;
            }
        }
    }

    /**
     * 初始化控件内容函数↓
     * *@param void
     */
    private void initViews() {
        String name = sp.getString("NAME", "姓名获取错误");
        String studentid = sp.getString("STUDENTID", "学号获取错误");
        String gender = sp.getString("GENDER","性别获取错误");
        String vcode = sp.getString("VCODE", "验证码获取错误");

        mBackIndividualBtn = (ImageView) findViewById(R.id.dorm_select_back_individual);
        mDormUpdateBtn = (ImageView) findViewById(R.id.dorm_select_update_btn);
        mConfirmBtn = (Button) findViewById(R.id.dorm_select_next_btn);
        mBackLoginFab = (FloatingActionButton) findViewById(R.id.fab);

        nameTv = (TextView) findViewById(R.id.dorm_select_name);
        studentidTv = (TextView) findViewById(R.id.dorm_select_studentid);
        genderTv = (TextView) findViewById(R.id.dorm_select_gender);
        vccodeTv = (TextView) findViewById(R.id.dorm_select_vcode);

        mate1IdET = (ClearEditText) findViewById(R.id.dorm_select_mate1_id);
        mate1CodeET = (ClearEditText) findViewById(R.id.dorm_select_mate1_code);
        mate2IdET = (ClearEditText) findViewById(R.id.dorm_select_mate2_id);
        mate2CodeET = (ClearEditText) findViewById(R.id.dorm_select_mate2_code);
        mate3IdET = (ClearEditText) findViewById(R.id.dorm_select_mate3_id);
        mate3CodeET = (ClearEditText) findViewById(R.id.dorm_select_mate3_code);

        nameTv.setText("姓名 ： " + name);
        studentidTv.setText("学号 ： " + studentid);
        genderTv.setText("性别 ： " + gender);
        vccodeTv.setText("校验码 ： " + vcode);

        dorm5RadioButton = (RadioButton) findViewById(R.id.dorm_select_5);
        dorm13RadioButton = (RadioButton) findViewById(R.id.dorm_select_13);
        dorm14RadioButton = (RadioButton) findViewById(R.id.dorm_select_14);
        dorm8RadioButton = (RadioButton) findViewById(R.id.dorm_select_8);
        dorm9RadioButton = (RadioButton) findViewById(R.id.dorm_select_9);
        dormNoRadioGroup = (RadioGroup) findViewById(R.id.dorm_select_radiogroup);

        dorm5RadioButton.setText("5号楼   ： 数据获取中");
        dorm13RadioButton.setText("13号楼 ： 数据获取中");
        dorm14RadioButton.setText("14号楼 ： 数据获取中");
        dorm8RadioButton.setText("8号楼   ： 数据获取中");
        dorm9RadioButton.setText("9号楼   ： 数据获取中");
        dormNoRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                SharedPreferences.Editor editor = sp.edit();
                if (checkedId == dorm5RadioButton.getId()){
                    editor.putString("BUILDINGNO", "5");
                    Toast.makeText(DormSelectActivity.this, "已选择5号楼！\n请核对床位数是否大等于办理人数！", Toast.LENGTH_SHORT).show();
                }
                if (checkedId == dorm13RadioButton.getId()){
                    editor.putString("BUILDINGNO", "13");
                    Toast.makeText(DormSelectActivity.this, "已选择13号楼！\n请核对床位数是否大等于办理人数！", Toast.LENGTH_SHORT).show();
                }
                if (checkedId == dorm14RadioButton.getId()){
                    editor.putString("BUILDINGNO", "14");
                    Toast.makeText(DormSelectActivity.this, "已选择14号楼！\n请核对床位数是否大等于办理人数！", Toast.LENGTH_SHORT).show();
                }
                if (checkedId == dorm8RadioButton.getId()){
                    editor.putString("BUILDINGNO", "8");
                    Toast.makeText(DormSelectActivity.this, "已选择8号楼！\n请核对床位数是否大等于办理人数！", Toast.LENGTH_SHORT).show();
                }
                if (checkedId == dorm9RadioButton.getId()){
                    editor.putString("BUILDINGNO", "9");
                    Toast.makeText(DormSelectActivity.this, "已选择9号楼！\n请核对床位数是否大等于办理人数！", Toast.LENGTH_SHORT).show();
                }
                editor.commit();
            }
        });
    }

    /**
     * 查询各个宿舍楼剩余空床数信息↓
     * @param genderNo
     */
    private void getRoom(String genderNo) {
        final String address = "https://api.mysspku.com/index.php/V1/MobileCourse/getRoom?gender=" + genderNo;
        Log.d("MyAPP", address);
        new Thread(new Runnable() {
            @Override
            public void run() {
                UserInfo userinfo = null;
                Request request = new Request.Builder()
                        .url(address)     //配置URL，GET方式
//                    .post(formBody)
                        .build();
                try{
                    Response response = client.newCall(request).execute();
                    if (!response.isSuccessful()) {
                        Log.d("MyApp","selectunsucc+" + response.toString());
                        throw new IOException("Unexpected code" + response);
                    }
                    //Log.d("MyApp","JSON+" + response.body().string());//！response.body().string()只可调用一次，多次报错！因此先用一个String接收
                    //因为response.body()也是挺大的，OkHttp不把它存储在内存中，就是你需要的时候就去读一次，只给你了内容，没有给引用，所以一次请求读一次
                    String jsonstr = response.body().string();
                    Log.d("MyApp","selectJSON+" + jsonstr);
                    userinfo = new UserInfo();  //解析获取到的JSON数据，保存到UserInfo的公有属性中
                    try {
                        JSONObject data = new JSONObject(jsonstr).getJSONObject("data");
                        userinfo.setDorm5(data.optString("5")); //使用optString而不使用getString由于部分数据为空防止报错
                        userinfo.setDorm13(data.optString("13"));
                        userinfo.setDorm14(data.optString("14"));
                        userinfo.setDorm8(data.optString("8"));
                        userinfo.setDorm9(data.optString("9"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    if (userinfo != null){
                        Log.d("MyAPP", userinfo.toString());
                        Message msg = new Message();    //在非主线程中获取到的用户个人信息传递给主线程更新UI控件
                        msg.what = UPDATE_ROOM_NUMBER;
                        msg.obj = userinfo;
                        mHandler.sendMessage(msg);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * updateDormNumber函数用于更新UI中的控件↓
     * *@object UserInfo
     */
    private void updateDormNumber(final UserInfo userinfo) {
        String dorm5number = userinfo.getDorm5();
        String dorm13number = userinfo.getDorm13();
        String dorm14number = userinfo.getDorm14();
        String dorm8number = userinfo.getDorm8();
        String dorm9number = userinfo.getDorm9();

        dorm5RadioButton.setText("5号楼   ： " + dorm5number + "个");
        dorm13RadioButton.setText("13号楼 ： " + dorm13number + "个");
        dorm14RadioButton.setText("14号楼 ： " + dorm14number + "个");
        dorm8RadioButton.setText("8号楼   ： " + dorm8number + "个");
        dorm9RadioButton.setText("9号楼   ： " + dorm9number + "个");

        if (dorm5number.equals("0"))
            dorm5RadioButton.setEnabled(false);
        if (dorm13number.equals("0"))
            dorm13RadioButton.setEnabled(false);
        if (dorm14number.equals("0"))
            dorm14RadioButton.setEnabled(false);
        if (dorm8number.equals("0"))
            dorm8RadioButton.setEnabled(false);
        if (dorm9number.equals("0"))
            dorm9RadioButton.setEnabled(false);
    }

    /**
     * 获取并保存所有办理信息后提交POST，返回成功或失败的boolean值↓
     * *@param void
     */
    private Boolean saveAndPostSelectRoom() {
        String mate1Id = mate1IdET.getText().toString();
        String mate1Code = mate1CodeET.getText().toString();
        if (!mate1Id.isEmpty() && !mate1Code.isEmpty())
            selectNum++;
        String mate2Id = mate2IdET.getText().toString();
        String mate2Code = mate2CodeET.getText().toString();
        if (!mate2Id.isEmpty() && !mate2Code.isEmpty())
            selectNum++;
        String mate3Id = mate3IdET.getText().toString();
        String mate3Code = mate3CodeET.getText().toString();
        if (!mate3Id.isEmpty() && !mate3Code.isEmpty())
            selectNum++;

        String stuid = sp.getString("STUDENTID", "1301210899");
        String buildingNo = sp.getString("BUILDINGNO", "");
        if (buildingNo.isEmpty())
            return false;

        final String address = "https://api.mysspku.com/index.php/V1/MobileCourse/SelectRoom";
        Log.d("MyAPP", address + " +POST：" +selectNum + ","  + stuid + "," + mate1Id + "," + mate1Code + ","
                + mate2Id + "," + mate2Code + "," + mate3Id + "," + mate3Code + "," + buildingNo);
        final RequestBody formBody = new FormBody.Builder()       //使用OKHttp的POST方法向服务器发送选择宿舍的办理信息（键值对）
                .add("num", String.valueOf(selectNum))
                .add("stuid", stuid)
                .add("stu1id", mate1Id)
                .add("v1code", mate1Code)
                .add("stu2id", mate2Id)
                .add("v2code", mate2Code)
                .add("stu3id", mate3Id)
                .add("v3code", mate3Code)
                .add("buildingNo", String.valueOf(buildingNo))
                .build();
        new Thread(new Runnable() {
            @Override
            public void run() {
                Request request = new Request.Builder()
                        .url(address)     //配置URL，GET方式
                        .post(formBody)
                        .build();
                try{
                    Response response = client.newCall(request).execute();
                    if (!response.isSuccessful()) {
                        Log.d("MyApp","postResponse+" + response.toString());
                        throw new IOException("Unexpected code" + response);
                    }
                    String testbody = response.body().string();
                    if (testbody.equals("{\"errcode\":0}")){
                        Log.d("MyApp","true+" + response.toString());
                        Log.d("MyApp","truebody+" + testbody);
                        SharedPreferences.Editor editor = sp.edit();
                        editor.putBoolean("SUCCESS", true);
                        editor.commit();
                    }
                    else {
                        Log.d("MyApp","false+" + response.toString());
                        Log.d("MyApp","falsebody+" + testbody);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        return true;
    }

}
