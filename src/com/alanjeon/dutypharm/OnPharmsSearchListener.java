package com.alanjeon.dutypharm;

import java.util.List;

/**
 * Created with IntelliJ IDEA. User: skyisle Date: 12. 6. 20. Time: 오전 10:50 To
 * change this template use File | Settings | File Templates.
 */
public interface OnPharmsSearchListener {

    void onFoundPharms(List<Pharm> found, boolean showMsg);

    void onError(String reason);

    void onTargetAddressChanged(String address, boolean retry);
}
