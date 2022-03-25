/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.yixun.pay;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;

import com.yixun.message.Message;
import com.yixun.tools.ToastUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BillingClientLifecycle implements PurchasesUpdatedListener,
        BillingClientStateListener, SkuDetailsResponseListener, PurchasesResponseListener {

    private static volatile BillingClientLifecycle instance;
    private static final String TAG = "BillingLifecycle --> ";

    public boolean isConnected = false;

    private final List<String> skuList = new ArrayList<String>();
    public Map<String, SkuDetails> SkusDetailList = new HashMap<String, SkuDetails>();
    private final Application app;
    private BillingClient billingClient;

    private BillingClientLifecycle(Application app) {
        this.app = app;
    }

    public static BillingClientLifecycle getInstance(Application app) {
        if (instance == null) {
            synchronized (BillingClientLifecycle.class) {
                if (instance == null) {
                    instance = new BillingClientLifecycle(app);
                }
            }
        }
        return instance;
    }

    @Override
    public void onBillingSetupFinished(BillingResult billingResult) {
        int responseCode = billingResult.getResponseCode();
        String debugMessage = billingResult.getDebugMessage();
        Message.UnityLog(TAG + "onBillingSetupFinished: " + responseCode + " " + debugMessage);
        if (responseCode == BillingClient.BillingResponseCode.OK) {
            isConnected = true;
            querySkuDetails();
            queryPurchases();
        }
    }

    @Override
    public void onBillingServiceDisconnected() {
        Message.UnityLog(TAG + "onBillingServiceDisconnected");
        isConnected = false;
    }

    @Override
    public void onSkuDetailsResponse(@NonNull BillingResult billingResult, List<SkuDetails> skuDetailsList) {
        int responseCode = billingResult.getResponseCode();
        Message.UnityLog(TAG + "onSkuDetailsResponse: " + responseCode);
        if (responseCode==BillingClient.BillingResponseCode.OK){
            final int expectedSkuDetailsCount = skuList.size();
            if (skuDetailsList == null) {
                Message.UnityLog(TAG + "onSkuDetailsResponse: " +
                        "Expected " + expectedSkuDetailsCount + ", " +
                        "Found null SkuDetails. " +
                        "Check to see if the SKUs you requested are correctly published " +
                        "in the Google Play Console.");
            } else {
                Message.UnityLog(TAG + "onSkuDetailsResponse skuDetails  count:" + skuDetailsList.size());
                for (SkuDetails skuDetails : skuDetailsList) {
                    Message.UnityLog(TAG + "onSkuDetailsResponse skuDetails :" + skuDetails.toString());
                    this.SkusDetailList.put(skuDetails.getSku(), skuDetails);
                }

            }
        } else {
            String debugMessage = billingResult.getDebugMessage();
            Message.UnityLog(TAG + "onSkuDetailsResponse:" + debugMessage);
        }
    }

    /**
     * Callback from the billing library when queryPurchasesAsync is called.
     */
    @Override
    public void onQueryPurchasesResponse(@NonNull BillingResult billingResult, @NonNull List<Purchase> list) {
        int responseCode = billingResult.getResponseCode();
        if (responseCode == BillingClient.BillingResponseCode.OK){
            processPurchases(list);
        } else {
            String debugMessage = billingResult.getDebugMessage();
            Message.UnityLog(TAG + "onQueryPurchasesResponse:" + debugMessage);
        }
    }

    /**
     * Called by the Billing Library when new purchases are detected.
     */
    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult, List<Purchase> purchases) {
        int responseCode = billingResult.getResponseCode();
        String debugMessage = billingResult.getDebugMessage();
        Message.UnityLog(TAG + String.format("onPurchasesUpdated: %s %s", responseCode, debugMessage));
        switch (responseCode) {
            case BillingClient.BillingResponseCode.OK:
                if (purchases == null) {
                    Message.UnityLog(TAG + "onPurchasesUpdated: null purchase list");
                    processPurchases(null);
                } else {
                    processPurchases(purchases);
                }
                break;
            case BillingClient.BillingResponseCode.USER_CANCELED:
                Message.UnityLog(TAG + "onPurchasesUpdated: User canceled the purchase");
                ToastUtil.makeTextLong(this.app,"USER_CANCELED");
                break;
            case BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED:
                Message.UnityLog(TAG + "onPurchasesUpdated: The user already owns this item");
                ToastUtil.makeTextShort(this.app,"ITEM_ALREADY_OWNED");
                break;
            case BillingClient.BillingResponseCode.DEVELOPER_ERROR:
                Message.UnityLog(TAG + "onPurchasesUpdated: Developer error means that Google Play " +
                        "does not recognize the configuration. If you are just getting started, " +
                        "make sure you have configured the application correctly in the " +
                        "Google Play Console. The SKU product ID must match and the APK you " +
                        "are using must be signed with release keys."
                );
                ToastUtil.makeTextShort(this.app,"DEVELOPER_ERROR");
                break;
            default:
                ToastUtil.makeTextShort(this.app,"PURCHASE_FAILED");
                break;
        }
    }

    //Call while start
    public void startConnection() {
        Message.UnityLog(TAG + "startConnection");
        if (isConnected) {
            Message.UnityLog(TAG + "BillingClient: is already  connected...");
            return;
        }
        billingClient = BillingClient.newBuilder(app)
                .setListener(this)
                .enablePendingPurchases() // Not used for subscriptions.
                .build();
        if (!billingClient.isReady()) {
            Message.UnityLog(TAG + "BillingClient: Start connection...");
            billingClient.startConnection(this);
        }
    }

    public void dispose() {
        if (billingClient.isReady()) {
            // After calling endConnection(), we must create a new BillingClient.
            billingClient.endConnection();
            isConnected = false;
        }
    }

    public void updateSkus(String[] skuIds) {
        this.skuList.clear();
        Collections.addAll(this.skuList, skuIds);
    }

    /**
     * In order to make purchases, you need the {@link SkuDetails} for the item or subscription.
     * This is an asynchronous call that will receive a result in {@link #onSkuDetailsResponse}.
     */
    public void querySkuDetails() {
        printQuerySku();
        SkuDetailsParams params = SkuDetailsParams.newBuilder()
                .setType(BillingClient.SkuType.INAPP)
                .setSkusList(skuList)
                .build();
        billingClient.querySkuDetailsAsync(params, this);
    }
    private void printQuerySku() {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < this.skuList.size(); i++) {
            str.append(" , ").append(this.skuList.get(i));
        }
        Message.UnityLog("querySkuDetailsAsync:" + str);
    }

    /**
     * Query Google Play Billing for existing purchases.
     * <p>
     * New purchases will be provided to the PurchasesUpdatedListener.
     * You still need to check the Google Play Billing API to know when purchase tokens are removed.
     */
    private void queryPurchases() {
        if (!billingClient.isReady()) {
            Message.UnityLog(TAG + "queryPurchases: BillingClient is not ready");
        }
        billingClient.queryPurchasesAsync(BillingClient.SkuType.INAPP, this);
    }

    /**
     * Send purchase SingleLiveEvent and update purchases LiveData.
     * <p>
     * The SingleLiveEvent will trigger network call to verify the subscriptions on the sever.
     * The LiveData will allow Google Play settings UI to update based on the latest purchase data.
     */
    private void processPurchases(List<Purchase> purchasesList) {
        if (isUnchangedPurchaseList(purchasesList)) {
            Message.UnityLog(TAG + "-->processPurchases: Purchase list has not changed");
            return;
        }
        if (purchasesList != null) {
            registerPurchases((purchasesList));
            logAcknowledgementStatus(purchasesList);
        }
    }
    /**
     * Check whether the purchases have changed before posting changes.
     */
    private boolean isUnchangedPurchaseList(List<Purchase> purchasesList) {
        // TODO: Optimize to avoid updates with identical data.
        return false;
    }
    /**
     * Register SKUs and purchase tokens with the server.
     */
    private void registerPurchases(List<Purchase> purchaseList) {
        for (Purchase purchase : purchaseList) {
            if (purchase.isAcknowledged()) {
                handlePurchase(purchase);
                continue;
            }
            String sku = purchase.getSkus().get(0);
            String purchaseToken = purchase.getPurchaseToken();

            Map<String, Object> table = new HashMap<String, Object>();
            table.put("purchaseToken", purchaseToken);
            table.put("sku", sku);
            Message.UnityCall("OnPurchaseOk",table);
        }
    }
    /**
     * Log the number of purchases that are acknowledge and not acknowledged.
     * <p>
     * https://developer.android.com/google/play/billing/billing_library_releases_notes#2_0_acknowledge
     * <p>
     * When the purchase is first received, it will not be acknowledge.
     * This application sends the purchase token to the server for registration. After the
     * purchase token is registered to an account, the Android app acknowledges the purchase token.
     * The next time the purchase list is updated, it will contain acknowledged purchases.
     */
    private void logAcknowledgementStatus(List<Purchase> purchasesList) {
        int ack_yes = 0;
        int ack_no = 0;
        for (Purchase purchase : purchasesList) {
            if (purchase.isAcknowledged()) {
                ack_yes++;
            } else {
                ack_no++;
            }
        }
        Message.UnityLog(TAG + " logAcknowledgementStatus: acknowledged=" + ack_yes + " unacknowledged=" + ack_no);
    }

    //Call while user click goods
    public void tryLaunchBilling(Activity activity,String sku){
        Message.UnityLog(TAG + "tryLaunchBilling: " + sku);
        SkuDetails skuDetails = this.SkusDetailList.get(sku);
        if (skuDetails == null) {
            Message.UnityLog(TAG +"Billing Could not find SkuDetails to make purchase.");
            openPlayStoreSubscriptions(activity,sku);
            return;
        }
        BillingFlowParams.Builder billingBuilder = BillingFlowParams.newBuilder().setSkuDetails(skuDetails);
        BillingFlowParams billingParams = billingBuilder.build();
        launchBillingFlow(activity, billingParams);
    }

    //open google store
    private void openPlayStoreSubscriptions(Activity activity,String sku){
        Message.UnityLog("Viewing subscriptions on the Google Play Store");
        String url;
        if (sku == null) {
            // If the SKU is not specified, just open the Google Play subscriptions URL.
            url = Constants.PLAY_STORE_SUBSCRIPTION_URL;
        } else {
            // If the SKU is specified, open the deeplink for this SKU on Google Play.
            url = String.format(Constants.PLAY_STORE_SUBSCRIPTION_DEEPLINK_URL, sku, app.getPackageName());
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        activity.startActivity(intent);
    }

    /**
     * Launching the billing flow.
     * Launching the UI to make a purchase requires a reference to the Activity.
     */
    private void launchBillingFlow(Activity activity, BillingFlowParams params) {
        if (!billingClient.isReady()) {
            Message.UnityLog(TAG + "launchBillingFlow: BillingClient is not ready");
        }
        BillingResult billingResult = billingClient.launchBillingFlow(activity, params);
        int responseCode = billingResult.getResponseCode();
        String debugMessage = billingResult.getDebugMessage();
        Message.UnityLog(TAG +  "launchBillingFlow: BillingResponse " + responseCode + " " + debugMessage);
    }

    //Call while server response ok
    public void consumePurchase(@NonNull String sku) {
        Message.UnityLog(TAG + "consumePurchase: " + sku);
        billingClient.queryPurchasesAsync(BillingClient.SkuType.INAPP, (billingResult, list) -> {
            if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                Message.UnityLog(TAG + "Problem getting purchases: " + billingResult.getDebugMessage());
            } else {
                for (Purchase purchase : list) {
                    for (String purchaseSku : purchase.getSkus())
                        if (purchaseSku.equals(sku)) {
                            handlePurchase(purchase);
                            return;
                        }
                }
            }
            Message.UnityLog(TAG + "Unable to consume SKU: " + sku + " Sku not found.");
        });
    }

    // Purchase retrieved from BillingClient#queryPurchasesAsync or your PurchasesUpdatedListener.
    private void handlePurchase(Purchase purchase) {
        Message.UnityLog(TAG + "handlePurchase:" + purchase.toString());
        ConsumeParams consumeParams =
                ConsumeParams.newBuilder()
                        .setPurchaseToken(purchase.getPurchaseToken())
                        .build();
        ConsumeResponseListener listener = (billingResult, purchaseToken) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                ToastUtil.makeTextShort(this.app,"PURCHASE_SUCCESS");
            }
        };
        billingClient.consumeAsync(consumeParams, listener);
    }


    /**
     * Acknowledge a purchase.
     * <p>
     * https://developer.android.com/google/play/billing/billing_library_releases_notes#2_0_acknowledge
     * <p>
     * Apps should acknowledge the purchase after confirming that the purchase token
     * has been associated with a user. This app only acknowledges purchases after
     * successfully receiving the subscription data back from the server.
     * <p>
     * Developers can choose to acknowledge purchases from a server using the
     * Google Play Developer API. The server has direct access to the user database,
     * so using the Google Play Developer API for acknowledgement might be more reliable.
     * TODO(134506821): Acknowledge purchases on the server.
     * <p>
     * If the purchase token is not acknowledged within 3 days,
     * then Google Play will automatically refund and revoke the purchase.
     * This behavior helps ensure that users are not charged for subscriptions unless the
     * user has successfully received access to the content.
     * This eliminates a category of issues where users complain to developers
     * that they paid for something that the app is not giving to them.
     */
    public void acknowledgePurchase(String purchaseToken) {
        AcknowledgePurchaseParams params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchaseToken)
                .build();
        billingClient.acknowledgePurchase(params, billingResult -> {
            int responseCode = billingResult.getResponseCode();
            String debugMessage = billingResult.getDebugMessage();
            Message.UnityLog(TAG + "acknowledgePurchase: " + responseCode + " " + debugMessage);
        });
    }
}
