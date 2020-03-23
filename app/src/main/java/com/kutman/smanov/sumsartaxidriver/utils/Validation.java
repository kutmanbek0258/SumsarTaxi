package com.kutman.smanov.sumsartaxidriver.utils;

import android.text.TextUtils;
import android.util.Patterns;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

public class Validation {

    public static boolean validateFields(String name){

        if (TextUtils.isEmpty(name)) {

            return false;

        } else {

            return true;
        }
    }

    public static boolean validateEmail(String string) {

        if (TextUtils.isEmpty(string) || !Patterns.EMAIL_ADDRESS.matcher(string).matches()) {

            return false;

        } else {

            return  true;
        }
    }

    public static boolean validatePhone(final String phone){
        //NOTE: This should probably be a member variable.
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();

        try
        {
            Phonenumber.PhoneNumber numberProto = phoneUtil.parse(phone, "KG");
            return phoneUtil.isValidNumber(numberProto);
        }
        catch (NumberParseException e)
        {
            System.err.println("NumberParseException was thrown: " + e.toString());
        }

        return false;
    }

    public static String convertToE164(String phone) {
        try {
            PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
            Phonenumber.PhoneNumber phoneNumber = phoneNumberUtil.parse(phone, "KG");
            if (phoneNumberUtil.isValidNumber(phoneNumber) && (
                    phoneNumberUtil.getNumberType(phoneNumber)
                            == PhoneNumberUtil.PhoneNumberType.MOBILE
                            || phoneNumberUtil.getNumberType(phoneNumber)
                            == PhoneNumberUtil.PhoneNumberType.FIXED_LINE_OR_MOBILE)) {
                return phoneNumberUtil.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.E164);
            } else {
                return "";
            }
        } catch (NumberParseException e) {
            return "";
        }
    }
}
