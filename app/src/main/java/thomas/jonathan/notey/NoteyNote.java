package thomas.jonathan.notey;

/*The individual notifications.*/
public class NoteyNote {
    int id;
    String note;
    int icon;
    int spinnerLoc;
    int imgBtnNum;
    String title;
    String iconName;
    String alarm;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public int getIcon() {
        return icon;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }

    public int getSpinnerLoc() {
        return spinnerLoc;
    }

    public void setSpinnerLoc(int spinnerLoc) {
        this.spinnerLoc = spinnerLoc;
    }

    public int getImgBtnNum() {
        return imgBtnNum;
    }

    public void setImgBtnNum(int imgBtnNum) {
        this.imgBtnNum = imgBtnNum;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getIconName() {
        return iconName;
    }

    public void setIconName(String iconName) {
        this.iconName = iconName;
    }

    public String getAlarm() {
        return alarm;
    }

    public void setAlarm(String alarm) {
        this.alarm = alarm;
    }
}
