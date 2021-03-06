/*
 * Copyright (C) 2008 The Android Open Source Project
 * Copyright (c) 2012-2013, The Linux Foundation. All rights reserved.
 * Not a Contribution.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.codeaurora.telephony.msim;

import android.app.AppOpsManager;
import android.content.Intent;
import android.net.Uri;
import android.provider.Telephony.Sms.Intents;
import android.telephony.cdma.CdmaSmsCbProgramData;
import android.telephony.SmsCbMessage;
import android.telephony.Rlog;

import com.android.internal.telephony.cdma.CdmaSMSDispatcher;
import com.android.internal.telephony.cdma.CDMAPhone;
import com.android.internal.telephony.cdma.SmsMessage;
import com.android.internal.telephony.MSimConstants;
import com.android.internal.telephony.PhoneBase;
import com.android.internal.telephony.SmsStorageMonitor;
import com.android.internal.telephony.SmsUsageMonitor;
import com.android.internal.telephony.ImsSMSDispatcher;

import java.util.ArrayList;

final class MSimCdmaSMSDispatcher extends CdmaSMSDispatcher {

    public MSimCdmaSMSDispatcher(PhoneBase phone, SmsStorageMonitor storageMonitor,
            SmsUsageMonitor usageMonitor, ImsSMSDispatcher imsSMSDispatcher) {
        super(phone, storageMonitor, usageMonitor, imsSMSDispatcher);
        Rlog.d(TAG, "MSimCdmaSMSDispatcher created");
    }

    /**
     * Dispatches standard PDUs to interested applications
     *
     * @param pdus The raw PDUs making up the message
     */
    @Override
    protected void dispatchPdus(byte[][] pdus) {
        Intent intent = new Intent(Intents.SMS_RECEIVED_ACTION);
        intent.putExtra("pdus", pdus);
        intent.putExtra("format", getFormat());
        intent.putExtra(MSimConstants.SUBSCRIPTION_KEY,
                 mPhone.getSubscription()); //Subscription information to be passed in an intent
        dispatch(intent, RECEIVE_SMS_PERMISSION, AppOpsManager.OP_RECEIVE_SMS);
    }

    /**
     * Dispatches port addressed PDUs to interested applications
     *
     * @param pdus The raw PDUs making up the message
     * @param port The destination port of the messages
     */
    @Override
    protected void dispatchPortAddressedPdus(byte[][] pdus, int port) {
        Uri uri = Uri.parse("sms://localhost:" + port);
        Intent intent = new Intent(Intents.DATA_SMS_RECEIVED_ACTION, uri);
        intent.putExtra("pdus", pdus);
        intent.putExtra("format", getFormat());
        intent.putExtra(MSimConstants.SUBSCRIPTION_KEY,
                 mPhone.getSubscription()); //Subscription information to be passed in an intent
        dispatch(intent, RECEIVE_SMS_PERMISSION, AppOpsManager.OP_RECEIVE_SMS);
    }

    /**
     * Dispatches cell broadcast  to interested applications
     *
     * @param message The destination message of cell broadcast
     */
    @Override
    protected void dispatchBroadcastMessage(SmsCbMessage message) {
        if (message.isEmergencyMessage()) {
            Intent intent = new Intent(Intents.SMS_EMERGENCY_CB_RECEIVED_ACTION);
            intent.putExtra("message", message);
            intent.putExtra(MSimConstants.SUBSCRIPTION_KEY,
                    mPhone.getSubscription()); //Subscription information to be passed in an intent
            dispatch(intent, RECEIVE_EMERGENCY_BROADCAST_PERMISSION, AppOpsManager.OP_RECEIVE_SMS);
        } else {
            Intent intent = new Intent(Intents.SMS_CB_RECEIVED_ACTION);
            intent.putExtra("message", message);
            intent.putExtra(MSimConstants.SUBSCRIPTION_KEY,
                    mPhone.getSubscription()); //Subscription information to be passed in an intent
            dispatch(intent, RECEIVE_SMS_PERMISSION, AppOpsManager.OP_RECEIVE_SMS);
        }
    }

    /**
     * Dispatch service category program data to the CellBroadcastReceiver app, which filters
     * the broadcast alerts to display.
     * @param sms the SMS message containing one or more
     * {@link android.telephony.cdma.CdmaSmsCbProgramData} objects.
     */
    protected void handleServiceCategoryProgramData(SmsMessage sms) {
        ArrayList<CdmaSmsCbProgramData> programDataList = sms.getSmsCbProgramData();
        if (programDataList == null) {
            Rlog.e(TAG, "handleServiceCategoryProgramData: program data list is null!");
            return;
        }

        Intent intent = new Intent(Intents.SMS_SERVICE_CATEGORY_PROGRAM_DATA_RECEIVED_ACTION);
        intent.putExtra(MSimConstants.SUBSCRIPTION_KEY,
                mPhone.getSubscription()); //Subscription information to be passed in an intent
        intent.putExtra("sender", sms.getOriginatingAddress());
        intent.putParcelableArrayListExtra("program_data", programDataList);
        dispatch(intent, RECEIVE_SMS_PERMISSION, AppOpsManager.OP_RECEIVE_SMS, mScpResultsReceiver);
    }
}
