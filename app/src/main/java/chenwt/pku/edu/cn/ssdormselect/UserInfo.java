package chenwt.pku.edu.cn.ssdormselect;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by Administrator on 2017/12/11.
 */

public class UserInfo {     //公共类用于保存共有用户个人信息数据，存入（setXXX）和读取（getXXX）都通过面向对象的方法来处理

    private SharedPreferences sp; //实例化SharedPreference对象，用于存储联网解析之后获取的所有数据

    private String studentid = "学号获取错误", name = "姓名获取错误", gender = "性别获取错误", vcode = "验证码获取错误",
            room = "宿舍号获取错误", building = "楼号获取错误", location = "校区获取错误", grade = "年级获取错误",
            dorm5 = "5号楼剩余空床数获取错误", dorm13 = "13号楼剩余空床数获取错误", dorm14 = "14号楼剩余空床数获取错误",
            dorm8 = "8号楼剩余空床数获取错误", dorm9 = "9号楼剩余空床数获取错误";

    public String getStudentid() {
        return studentid;
    }

    public String getName() {
        return name;
    }

    public String getGender() {
        return gender;
    }

    public String getVcode() {
        return vcode;
    }

    public String getRoom() {
        return room;
    }

    public String getBuilding() {
        return building;
    }

    public String getLocation() {
        return location;
    }

    public String getGrade() {
        return grade;
    }

    public String getDorm5() {
        return dorm5;
    }

    public String getDorm13() {
        return dorm13;
    }

    public String getDorm14() {
        return dorm14;
    }

    public String getDorm8() {
        return dorm8;
    }

    public String getDorm9() {
        return dorm9;
    }


    public void setStudentid(String studentid) {
        this.studentid = studentid;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public void setVcode(String vcode) {
        this.vcode = vcode;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public void setBuilding(String building) {
        this.building = building;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public void setDorm5(String dorm5) {
        this.dorm5 = dorm5;
    }

    public void setDorm13(String dorm13) {
        this.dorm13 = dorm13;
    }

    public void setDorm14(String dorm14) {
        this.dorm14 = dorm14;
    }

    public void setDorm8(String dorm8) {
        this.dorm8 = dorm8;
    }

    public void setDorm9(String dorm9) {
        this.dorm9 = dorm9;
    }

    //用于存储联网解析之后获取的所有数据↓
    public void saveAllData(Activity activity){
        sp = activity.getSharedPreferences("userInfo", Context.MODE_WORLD_READABLE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("NAME", name);
        editor.putString("GENDER", gender);
        editor.putString("VCODE", vcode);
//        editor.putString("ROOM", room);
//        editor.putString("BUILDING", building);
//        editor.putString("LOCATION", location);
//        editor.putString("GRADE", grade);
        editor.commit();
    }

}
