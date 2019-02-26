package cn.net.leading.mynetworkstatesapplication;

import android.app.Activity;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import com.google.android.material.snackbar.Snackbar;


/**
 * 用来展示SnackBar提示用户的工具类
 * Created by: ZJ
 * Date: 2017-03-07
 * Time: 11:09
 *
 * @author zj
 */
public class SnackBarUtil {
    private static final String TAG = "SnackBarUtil";

    private SnackBarUtil() {
    }

    public static SnackBarUtil getInstance() {
        return SnackBarUtilSingletonHolder.INSTANCE;
    }

    /**
     * 向Snackbar中添加一个取消按钮
     *
     * @param snackbar
     * @param layoutId
     * @param index    新加布局在Snackbar中的位置
     */
    public static void addCancelButtonToSnackBar(final Snackbar snackbar, int layoutId, int index) {
        View snackBarView = snackbar.getView();
        Snackbar.SnackbarLayout snackbarLayout = (Snackbar.SnackbarLayout) snackBarView;

        Button add_view = (Button) LayoutInflater.from(snackBarView.getContext()).inflate(layoutId, null);

        add_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                snackbar.dismiss();
            }
        });

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.gravity = Gravity.CENTER_VERTICAL;

        ((LinearLayout) snackbarLayout.getChildAt(0)).addView(add_view, index, p);
    }

    /**
     * 展示断网提示SnackBar
     *
     * @param activity 需要展示提示的activity
     * @return 断网提示snackBar, 找不到activity的根布局会返回null
     */
    public Snackbar showNoAvailableNetworkSnackBar(final Activity activity) {
        ViewGroup activityViewGroup = activity.findViewById(android.R.id.content);
        Snackbar networkStatusSnackBar = null;
        if (activityViewGroup != null) {
            networkStatusSnackBar = Snackbar.make(activityViewGroup.getChildAt(0),
                    "网络连接已断开(右滑解除)", Snackbar.LENGTH_INDEFINITE)
                    .setAction("前往设置", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // 开启设置界面
                            activity.startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                        }
                    });
            networkStatusSnackBar.getView().setBackgroundColor(activity.getResources().getColor(R.color.colorSnackBarBackground));
            addCancelButtonToSnackBar(networkStatusSnackBar, R.layout.snack_bar_cacel_button_layout, 2);
            networkStatusSnackBar.show();
        } else {
            Log.e(TAG, "showNoAvailableNetworkSnackBar: 没有找到显示SnackBar的 ViewGroup");
        }
        return networkStatusSnackBar;
    }

    private static class SnackBarUtilSingletonHolder {
        private static final SnackBarUtil INSTANCE = new SnackBarUtil();
    }

}
