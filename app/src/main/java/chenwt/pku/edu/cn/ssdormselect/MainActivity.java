package chenwt.pku.edu.cn.ssdormselect;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private SharedPreferences sp; //实例化SharedPreference对象，用于取出所存用户名

    private static final int UPDATE_USER_INFO = 1;

    private TextView studentidTv, nameTv, genderTv, vccodeTv, roomTv, buildingTv, locationTv, gradeTv, overtv;
    private ImageView mUpdateBtn, mBackLoginBtn, mAttentionOrMap;
    private Button mNextBtn;

    private OkHttpClient client;

    private Handler mHandler = new Handler(){   //线程间消息处理机制，MessageQueue是一个存放消息对象的队列
        public void handleMessage(Message msg){
            switch (msg.what){      //消息对象的属性.what只能放数字，用作判断是哪个非主线程传来的消息
                case UPDATE_USER_INFO:
                    updateUserInfo((UserInfo) msg.obj); //在主线程中更新控件信息
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_individual);

        //获得sp实例对象
        sp = this.getSharedPreferences("userInfo", Context.MODE_WORLD_READABLE);

        initViews();    //初始化控件显示

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

        String studentid = sp.getString("STUDENTID", "1301210899");
        queryIndividualInf(studentid);

        mUpdateBtn.setOnClickListener(this);     //为刷新的图片控件设置监听事件
        mBackLoginBtn.setOnClickListener(this);   //为显示城市列表的图片控件设置监听事件
        mNextBtn.setOnClickListener(this);

        //onCreate方法的小尾巴
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.title_update_btn){    //如果点击到刷新，取出保存的学号（默认1700000000）去获取用户个人信息
            String studentid = sp.getString("STUDENTID", "1301210899");
            queryIndividualInf(studentid);
            Toast.makeText(MainActivity.this, "个人信息更新成功！", Toast.LENGTH_SHORT).show();
        }
        if (v.getId() == R.id.title_back_login){   //如果点击到返回登录界面，则弹出登录界面到前端
            SharedPreferences.Editor editor = sp.edit();
            editor.putBoolean("isREMEMBER",false);
            editor.putBoolean("isLOAD",false);
            editor.commit();
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
        if (v.getId() == R.id.individual_next_btn){   //如果点击到开始办理住宿，则弹出后续的办理住宿界面到前端
            Intent intent = new Intent(this, DormSelectActivity.class);
            startActivity(intent);
            finish();
        }
    }

    /**
     * 初始化控件内容函数↓
     * *@param void
     */
    private void initViews() {
        mUpdateBtn = (ImageView) findViewById(R.id.title_update_btn);   //绑定刷新的图片控件
        mBackLoginBtn = (ImageView) findViewById(R.id.title_back_login); //绑定返回登录界面的图片控件
        mAttentionOrMap = (ImageView) findViewById(R.id.attention_or_map);  //绑定显示注意事项或地图的图片控件
        mNextBtn = (Button) findViewById(R.id.individual_next_btn);     //绑定确认下一步的图片控件
        overtv = (TextView) findViewById(R.id.individual_over_tv);

        studentidTv = (TextView) findViewById(R.id.individual_studentid);
        nameTv = (TextView)findViewById(R.id.individual_name);
        genderTv = (TextView) findViewById(R.id.individual_gender);
        vccodeTv = (TextView) findViewById(R.id.individual_vcode);
        roomTv = (TextView) findViewById(R.id.individual_room);
        buildingTv = (TextView) findViewById(R.id.individual_building);
        locationTv = (TextView) findViewById(R.id.individual_location);
        gradeTv = (TextView) findViewById(R.id.individual_grade);

        studentidTv.setText("学号 ： 获取中...");
        nameTv.setText("姓名 ： 获取中...");
        genderTv.setText("性别 ： 获取中...");
        vccodeTv.setText("校验码 ： 获取中...");
        roomTv.setText("宿舍号 ： 获取中...");
        buildingTv.setText("楼号 ： 获取中...");
        locationTv.setText("校区 ： 获取中...");
        gradeTv.setText("年级 ： 获取中...");
    }

    /**
     * 查询个人信息↓
     * @param studentid
     */
    private void queryIndividualInf(String studentid){
        final String address = "https://api.mysspku.com/index.php/V1/MobileCourse/getDetail?stuid=" + studentid;
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
                        Log.d("MyApp","unsucc+" + response.toString());
                        throw new IOException("Unexpected code" + response);
                    }
                    //Log.d("MyApp","JSON+" + response.body().string());//！response.body().string()只可调用一次，多次报错！因此先用一个String接收
                    //因为response.body()也是挺大的，OkHttp不把它存储在内存中，就是你需要的时候就去读一次，只给你了内容，没有给引用，所以一次请求读一次
                    String jsonstr = response.body().string();
                    Log.d("MyApp","JSON+" + jsonstr);
                    userinfo = parseJson(jsonstr);  //解析获取到的JSON数据，保存到UserInfo的公有属性中
                    if (userinfo != null){
                        Log.d("MyAPP", userinfo.toString());
                        Message msg = new Message();    //在非主线程中获取到的用户个人信息传递给主线程更新UI控件
                        msg.what = UPDATE_USER_INFO;
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
     * 解析返回的JSON数据，并保存到用于存取用户个人信息的公共类UserInfo中↓
     * @param jsonstr
     */
    private UserInfo parseJson(String jsonstr) {
        UserInfo userinfo = new UserInfo();
        try {
            JSONObject data = new JSONObject(jsonstr).getJSONObject("data");
//            String studentid = data.optString("studentid");
//            String name = data.optString("name");
//            String gender = data.optString("gender");
//            String vcode = data.optString("vcode");
//            String room = data.optString("room");
//            String building = data.optString("building");
//            String location = data.optString("location");
//            String grade = data.optString("grade");
//            Log.d("MyApp","parseJson：" + "学号："+ studentid + ", 姓名：" + name + ",性别：" + gender + "验证码："+ vcode +
//                    ", 宿舍号：" + room + ",楼号 ：" + building + ", 校区：" + location + ",年级 ：" + grade);
            userinfo.setStudentid(data.optString("studentid")); //使用optString而不使用getString由于部分数据为空防止报错
            userinfo.setName(data.optString("name"));
            userinfo.setGender(data.optString("gender"));
            userinfo.setVcode(data.optString("vcode"));
            userinfo.setRoom(data.optString("room"));
            userinfo.setBuilding(data.optString("building"));
            userinfo.setLocation(data.optString("location"));
            userinfo.setGrade(data.optString("grade"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        userinfo.saveAllData(this);
        return userinfo;
    }

    /**
     * updateUserInfo函数用于更新UI中的控件↓
     * *@object UserInfo
     */
    private void updateUserInfo(UserInfo userinfo) {
        String room = userinfo.getRoom();
        String building = userinfo.getBuilding();

        if (room.equals("") && building.equals("")){
            mNextBtn.setVisibility(View.VISIBLE);
        }
        else {
            overtv.setVisibility(View.VISIBLE);
            roomTv.setText("宿舍号 ： " + room);
            buildingTv.setText("楼号 ： " + building);
            roomTv.setVisibility(View.VISIBLE);
            buildingTv.setVisibility(View.VISIBLE);
            mAttentionOrMap.setImageResource(R.drawable.sspku_map);
        }

        studentidTv.setText("学号 ： " + userinfo.getStudentid());
        nameTv.setText("姓名 ： " + userinfo.getName());
        genderTv.setText("性别 ： " + userinfo.getGender());
        vccodeTv.setText("校验码 ： " + userinfo.getVcode());
        locationTv.setText("校区 ： " + userinfo.getLocation());
        gradeTv.setText("年级 ： " + userinfo.getGrade());
    }

}
