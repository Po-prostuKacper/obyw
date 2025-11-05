package whisk.utils;

public abstract class TokenCheck {

    private boolean safe = true;

    public abstract void call(String token);

    public void setSafe(boolean safe){
        this.safe = safe;
    }

    public boolean getSafe(){
        return safe;
    }

}
