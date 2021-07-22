package org.noear.water;

import org.noear.snack.ONode;
import org.noear.solon.Solon;
import org.noear.solon.Utils;
import org.noear.solon.core.NvMap;
import org.noear.solon.core.handle.Context;
import org.noear.water.log.Logger;
import org.noear.water.log.WaterLogger;
import org.noear.water.utils.IPUtils;
import org.noear.water.utils.RuntimeStatus;
import org.noear.water.utils.RuntimeUtils;
import org.noear.water.utils.TextUtils;

import java.io.IOException;

//
// Water for service project adapter
//
abstract class WaterAdapterBase extends AbstractWaterAdapter {
    static Logger logger = WaterLogger.get(WW.water_log_upstream, WaterAdapterBase.class);

    NvMap service_args;
    private String _localHost;
    private String _note = "";

    public WaterAdapterBase(NvMap args, int port) {
        service_args = args;
        service_port = port;

        onInit();
    }

    protected void onInit() {
        if (Solon.cfg().isSetupMode()) {
            return;
        }

        registerService();
        messageSubscribe();
    }

    /**
     * 当前服务的本地地址(host:port)
     */
    @Override
    public String localHost() {
        return _localHost;
    }

    @Override
    protected void registerService() {
        _localHost = getLocalAddress(this.service_port);

        //为client配置local host
        WaterClient.localHostSet(_localHost);
        WaterClient.localServiceSet(service_name());

        if (service_args == null || service_args.size() == 0) {
            _note = "";
        } else {
            ONode tmp = ONode.load(service_args);
            tmp.remove("server.port");
            _note = tmp.toJson();
            if (_note.length() < 3) {
                _note = "";
            }
        }

        String code_location = Solon.cfg().sourceLocation().getPath();

        if (service_port > 0) {
            WaterClient.Registry.register(Solon.cfg().appGroup(), this.service_name(), _localHost, _note, WW.path_run_check, 0, this.alarm_mobile(), code_location, is_unstable());
        } else {
            WaterClient.Registry.register(Solon.cfg().appGroup(), this.service_name(), _localHost, _note, WW.path_run_check, 1, this.alarm_mobile(), code_location, is_unstable());
        }
    }

    /**
     * 设置状态
     */
    public void stateSet(boolean enabled) {
        if (TextUtils.isEmpty(_localHost) == false) {
            WaterClient.Registry.set(this.service_name(), _localHost, _note, enabled);
        }
    }

    @Override
    public void cacheUpdateHandler(String tag) {
        super.cacheUpdateHandler(tag);
        String[] ss = tag.split(":");
        if ("upstream".equals(ss[0])) {
            WaterUpstream tmp = WaterUpstream.getOnly(ss[1]);
            if (tmp != null) {
                try {
                    tmp.reload();
                } catch (Exception ex) {
                    ex.printStackTrace();//最后日志记录到服务端
                    logger.error(ss[1], "reload", "", ex);
                }
            }
        }
    }

    //2.1.提供服务供查入口 //必须重写
    public String serviceCheck(Context cxt) {
        String ups = cxt.param("upstream");

        if (TextUtils.isEmpty(ups) == false) {
            //用于检查负责的情况
            ONode odata = new ONode().asObject();

            if ("*".equals(ups)) {
                WaterUpstream._map.forEach((k, v) -> {
                    ONode n = odata.get(k);

                    n.set("service", k);
                    ONode nl = n.get("upstream").asArray();
                    v._nodes.forEach((s) -> {
                        nl.add(s);
                    });
                });
            } else {
                WaterUpstream v = WaterUpstream.getOnly(ups);
                if (v != null) {
                    ONode n = odata.get(ups);

                    n.set("service", ups);
                    n.set("agent", v.agent());
                    n.set("policy", v.policy());
                    ONode nl = n.get("upstream").asArray();
                    v._nodes.forEach((s) -> {
                        nl.add(s);
                    });
                }
            }

            return odata.toJson();
        }

        return "{\"code\":1}";
    }

    //::可以重写，且需要RequestMapping
    //2.2.接收消息
    public String messageReceive(Context cxt) throws Throwable {
        return doMessageReceive(k -> cxt.param(k));
    }

    public void handle(Context ctx) throws IOException {
        String path = ctx.path();

        String text = "";
        try {
            if (WW.path_run_check.equals(path)) {
                //run/check/
                text = serviceCheck(ctx);
            } else if (WW.path_run_stop.equals(path)) {
                //run/stop/
                String ip = IPUtils.getIP(ctx);
                if (authIp(ip)) {
                    stateSet(false);
                    Solon.stop();
                    text = "OK";
                } else {
                    text = (ip + ",not is whitelist!");
                }
            } else if (WW.path_run_status.equals(path)) {
                //run/status/
                String ip = IPUtils.getIP(ctx);
                if (authIp(ip)) {
                    RuntimeStatus rs = RuntimeUtils.getStatus();
                    rs.name = WaterAdapter.global().service_name();
                    rs.address = WaterAdapter.global().localHost();

                    text = ONode.stringify(rs);
                } else {
                    text = (ip + ",not is whitelist!");
                }
            } else if (WW.path_run_job.equals(path)) {
                //run/job/ 暂不支持
            } else if (WW.path_msg_receiver.equals(path)) {
                //msg/receive
                text = messageReceive(ctx);
            }
        } catch (Throwable ex) {
            text = Utils.getFullStackTrace(ex);
            ex.printStackTrace();
        }

        ctx.output(text);
    }

    private boolean authIp(String ip) {
        if (Solon.cfg().isDriftMode()) {
            return true;
        } else {
            return WaterClient.Whitelist.existsOfMasterIp(ip);
        }
    }
}
