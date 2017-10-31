package chenwt.pku.edu.cn.ssdormselect;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.app.LoaderManager.LoaderCallbacks;

import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;

import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.Manifest.permission.READ_CONTACTS;

/**
 * A login screen that offers login via student_id/password.
 */
public class LoginActivity extends AppCompatActivity implements LoaderCallbacks<Cursor> {

    /**
     * Id to identity READ_CONTACTS permission request.
     */
    private static final int REQUEST_READ_CONTACTS = 0;

    /**
     * A dummy authentication store containing known user names and passwords.
     * TODO: remove after connecting to a real authentication system.
     ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓*/
    private static final String[] DUMMY_CREDENTIALS = new String[]{     //DUMMY_CREDENTIALS用于模拟可通过验证的："账户:密码"
            "1701210346:hello", "1700000000:world"
    };

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;     //mAuthTask是UserLoginTask的实例，UserLoginTask继承自AsyncTask，后面会提到这个的用处

    // UI references.
    private AutoCompleteTextView mStudentIDView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;

    private SharedPreferences sp; //实例化SharedPreference对象，用于存储记住密码所保存的用户名和密码

    private final OkHttpClient client;
    public LoginActivity() throws Exception {
        client = new OkHttpClient.Builder()
                .connectTimeout(2, TimeUnit.SECONDS)        //设置连接超时时间为2s
/*                .writeTimeout(10, TimeUnit.SECONDS)         //设置读取的超时时间
                .readTimeout(30, TimeUnit.SECONDS)          //设置写入的超时时间*/
                .build();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        // Set up the login form.

        //获得sp实例对象
        sp = this.getSharedPreferences("userInfo", Context.MODE_WORLD_READABLE);

        //先判断记住密码是否被勾选中，如果被选中则判断是否登陆过，如果登陆过，直接登陆
        Intent intent = new Intent(this,MainActivity.class);
//        intent.putExtra("com.witt.administrator.dininggo.USER",sp.getString("USER_NAME",""));
        if (sp.getBoolean("isREMEMBER",false)){
            if (sp.getBoolean("isLOAD",false)){     //.getBoolean是取出"键"所存的Boolean值，第一个参数是键名，第二个参数是默认值
                startActivity(intent);
                this.finish();  //关闭登陆界面
            }
        }

        mStudentIDView = (AutoCompleteTextView) findViewById(R.id.student_id);
        populateAutoComplete();     //按方法名来看是构造自动补全的列表，跟进（Ctrl + 鼠标左键）

        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {   //这是EditText的回车事件（回车触发登陆）
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin(); //登陆
                    return true;
                }
                return false;
            }
        });

        CheckBox mRememberPWDCheckBox = (CheckBox) findViewById(R.id.rememberPWDId);
        mRememberPWDCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putBoolean("isREMEMBER",isChecked);
                    editor.commit();
                }
            }
        });

        Button mStudentIDSignInButton = (Button) findViewById(R.id.student_id_sign_in_button);
        mStudentIDSignInButton.setOnClickListener(new OnClickListener() {   //登陆按钮
            @Override
            public void onClick(View view) {
                attemptLogin(); //登陆
            }
        });

        //↓下面这两个用于获取显示的View，在登录的时候可以进行登录窗口gone，ProgressBar visible的操作
        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
    }

    private void populateAutoComplete() {//先是通过mayRequestContacts判断是否继续执行，若通过判断则初始化Loaders，通过Loaders后台异步读取用户的账户信息
        if (!mayRequestContacts()) {
            return;
        }

        getLoaderManager().initLoader(0, null, this);
    }

    private boolean mayRequestContacts() {      //该方法用于请求用户以获取读取账户的权限，主要是为了适配6.0新的权限机制
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (checkSelfPermission(READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        if (shouldShowRequestPermissionRationale(READ_CONTACTS)) {
            Snackbar.make(mStudentIDView, R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok, new View.OnClickListener() {
                        @Override
                        @TargetApi(Build.VERSION_CODES.M)
                        public void onClick(View v) {
                            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
                        }
                    });
        } else {
            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
        }
        return false;
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                populateAutoComplete();
            }
        }
    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid student_id number, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {   //初步判断输入的账号密码的合法性（是否为空，长度是否过小），并给出错误提示。↓
        if (mAuthTask != null) {    //↑通过初步检验后，隐藏登录框和按钮，显示进度条，并在AsyncTask中进行后台登录↓
            return;                 //↑这个AsyncTask就是上面的变量中的mAuthTask，使用的时候改写doInBackground方法实现网络验证用户名密码
        }

        // Reset errors.
        mStudentIDView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String studentid = mStudentIDView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.←模板居然允许密码为空
        //↓重新改写了，不允许密码为空
        if (TextUtils.isEmpty(password)){
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
        }
        else if (!isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid student_id number.
        if (TextUtils.isEmpty(studentid)) {
            mStudentIDView.setError(getString(R.string.error_field_required));
            focusView = mStudentIDView;
            cancel = true;
        } else if (!isStudentIDValid(studentid)) {
            mStudentIDView.setError(getString(R.string.error_invalid_student_id));
            focusView = mStudentIDView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mAuthTask = new UserLoginTask(studentid, password);
            mAuthTask.execute((Void) null);
        }
    }

    //↓判断用户输入的学号是否合法，后面可以再更新学号的正则表达式（regExp,Regular Expression，规则表达式）
    private boolean isStudentIDValid(String studentid) {

/*        //TODO: Replace this with your own logic
        return studentid.contains("@");//原来的模板用的是电子邮箱的判断    */

        /**
         * 大陆手机号码11位数，匹配格式：前三位固定格式+后8位任意数
         * 此方法中前三位格式有：
         * 13+任意数
         * 15+除4的任意数
         * 18+除1和4的任意数（自己用的181，加了个181）
         * 17+除9的任意数
         * 147
         */
//        String regExp = "^((13[0-9])|(15[^4])|(18[0-3,5-9])|(17[0-8])|(147))\\d{8}$";
//        Pattern p = Pattern.compile(regExp);
//        Matcher m = p.matcher(studentid);
//        return m.matches();
        return studentid.contains("17");
    }


    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() > 4;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this,
                // Retrieve data rows for the device user's 'profile' contact.
                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

                // Select only student_id number.
                ContactsContract.Contacts.Data.MIMETYPE +
                        " = ?", new String[]{ContactsContract.CommonDataKinds.Phone
                .CONTENT_ITEM_TYPE},

                // Show primary StudentID addresses first. Note that there won't be
                // a primary StudentID address if the user hasn't specified one.
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        List<String> studentids = new ArrayList<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            studentids.add(cursor.getString(ProfileQuery.ADDRESS));
            cursor.moveToNext();
        }

        addStudentIDsToAutoComplete(studentids);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    private void addStudentIDsToAutoComplete(List<String> studentIDAddressCollection) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(LoginActivity.this,
                        android.R.layout.simple_dropdown_item_1line, studentIDAddressCollection);

        mStudentIDView.setAdapter(adapter);
    }


    private interface ProfileQuery {
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.IS_PRIMARY,
        };

        int ADDRESS = 0;
        int IS_PRIMARY = 1;
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mStudentID;
        private final String mPassword;

        UserLoginTask(String studentid, String password) {
            mStudentID = studentid;
            mPassword = password;
        }

        /**
         * ↓复写AsyncTask.java类中的doInBackground方法，这是一个WorkerThread，可在其中进行访问网络验证账号密码的耗时操作
         * 这个方法return的是一个Boolean值，该方法执行完毕后启动的是AsyncTask类的onPostExecute方法接收该Boolean值
         */
        @Override
        protected Boolean doInBackground(Void... params) {  //Void... params 等价于Void[] params，即多个参数？
/*            // TODO: attempt authentication against a network service.
            try {
                // Simulate network access.
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                return false;
            }*/

            RequestBody formBody = new FormBody.Builder()       //使用OKHttp向服务器验证账号和密码
                    .add("user",mStudentID)
                    .add("pwd",mPassword)
                    .build();
            Request request = new Request.Builder()
                    .url("http://192.168.0.112:8080/DGAccount.jsp")     //配置URL
                    .post(formBody)
                    .build();
            try {
                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code" + response);
                }
                if (response.body().string().equals("true")){
                    return true;
                }
                else {
                    return false;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            for (String credential : DUMMY_CREDENTIALS) {
                String[] pieces = credential.split(":");            //非常聪明的用法，通过:（冒号）拆分一个字符串成pieces[0]和pieces[1]
                if (pieces[0].equals(mStudentID)) {
                    // Account exists, return true if the password matches.
                    return pieces[1].equals(mPassword);
                }
            }

            // TODO: register the new account here.
            return false;//以上都不通过时返回false弹出重新输入账号密码的提示，也可在这写上注册新用户的代码（注意逻辑）
        }

        /**
         * ↓复写AsyncTask.java类中的onPostExecute方法，这是MainThread，该方法接收doInBackground方法return的Boolean值
         */
        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(false);

            if (success) {
                //调用uiHandler，执行uiHandler的handleMessage方法
                Message msg = new Message();
                uiHandler.sendMessage(msg);
                finish();       //杀死LoginActivity
            } else {
                mPasswordView.setError(getString(R.string.error_incorrect_password));
                mPasswordView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }

        private Handler uiHandler = new Handler(){      //用于记住用户名密码以及做Activity跳转的程序
            @Override
            public void handleMessage(Message msg) {
                //记住用户名、密码、记住密码状态
                SharedPreferences.Editor editor = sp.edit();
                editor.putString("USER_NAME",mStudentID);
                editor.putString("PASSWORD",mPassword);
                editor.putBoolean("isLOAD",true);
                editor.commit();

                //Activity跳转，从登录界面跳转到主界面
                Intent intent = new Intent(LoginActivity.this,MainActivity.class);
                startActivity(intent);
            }
        };

/*        public boolean volley_POST_user(){
            String url = "http://192.168.0.112:8080/DGAccount.jsp";
            RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
            //Volley request，参数：请求方式，请求的URL，请求成功的回调，请求失败的回调
            StringRequest stringRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
                *//**
         * 请求成功的回调
         * @param response 请求返回的数据
         *//*
                @Override
                public void onResponse(String response) {
                    //TODO：处理返回结果
                    if (response.equals("true")){
                        result = true;
                    }
                    else {
                        result = false;
                    }
                    Log.d("### onResponse","POST_StringRequest -> " + response);
                }
            }, new Response.ErrorListener() {
                *//**
         * 请求失败的回调
         * @param error VolleyError
         *//*
                @Override
                public void onErrorResponse(VolleyError error) {
                    //TODO：处理错误
                    Log.e("### onResponse","POST_StringRequest -> response" + error.toString());
                }
            }){
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {

                    Map<String, String> paraMap = new HashMap<>();
                    //TODO：处理POST参数（在这里设置需要POST的参数）
                    paraMap.put("user", mStudentID);
                    paraMap.put("pwd", mPassword);
                    return paraMap;
                }
            };
            requestQueue.add(stringRequest);
            return result;
        }*/

    }
}

