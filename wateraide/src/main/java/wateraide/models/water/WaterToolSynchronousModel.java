package wateraide.models.water;

import org.noear.weed.annotation.PrimaryKey;
import org.noear.weed.annotation.Table;

import java.util.Date;

/**
 * @author noear 2021/11/3 created
 */
@Table("water_tool_synchronous")
public class WaterToolSynchronousModel {
    /**  */
    @PrimaryKey
    public int sync_id;
    /**  */
    public String tag;
    /**  */
    public String key;
    /**  */
    public String name;
    /** 0,增量同步；1,更新同步； */
    public int type;
    /** 间隔时间（秒） */
    public int interval;
    /**  */
    public String target;
    /**  */
    public String target_pk;
    /** 数据源模型 */
    public String source_model;
    /** 同步标识（用于临时存数据） */
    public long task_tag;
    /**  */
    public String alarm_mobile;
    /**  */
    public String alarm_sign;
    /**  */
    public int is_enabled;
    /**  */
    public Date last_fulltime;
}