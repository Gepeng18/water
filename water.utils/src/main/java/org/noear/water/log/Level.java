package org.noear.water.log;

public enum Level {
    TRACE(1),
    DEBUG(2),
    INFO(3),
    WARN(4),
    ERROR(5);

    public final int code;

    public static Level of(int code){
        for(Level v : values()){
            if(v.code == code){
                return v;
            }
        }
        //默认值
        return INFO;
    }

    Level(int code) {
        this.code = code;
    }
}
