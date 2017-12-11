package chenwt.pku.edu.cn.ssdormselect;

/**
 * Created by Administrator on 2017/12/11.
 */

public class UserInfo {     //公共类用于保存共有用户个人信息数据，存入（setXXX）和读取（getXXX）都通过面向对象的方法来处理
    private String studentid = "学号获取错误", name = "姓名获取错误", gender = "性别获取错误", vcode = "验证码获取错误",
            room = "宿舍号获取错误", building = "楼号获取错误", location = "校区获取错误", grade = "年级获取错误";

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
}
