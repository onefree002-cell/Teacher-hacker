package com.example.util

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract

data class PickedContactInfo(
    val name: String,
    val phoneNumber: String
) {
    val phone: String get() = phoneNumber
}

object ContactPickerHelper {

    fun extractContact(context: Context, contactUri: Uri): PickedContactInfo? = extractContactInfo(context, contactUri)

    /**
     * Extracts contact display name and clean phone number from a picked contact URI.
     */
    fun extractContactInfo(context: Context, contactUri: Uri): PickedContactInfo? {
        var name = ""
        var phoneNumber = ""

        try {
            val contentResolver = context.contentResolver

            // 1. Try querying as Phone URI (ContactsContract.CommonDataKinds.Phone)
            val phoneCursor: Cursor? = contentResolver.query(
                contactUri,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID
                ),
                null,
                null,
                null
            )

            phoneCursor?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                    if (nameIndex != -1) {
                        name = cursor.getString(nameIndex) ?: ""
                    }
                    if (numberIndex != -1) {
                        phoneNumber = cursor.getString(numberIndex) ?: ""
                    }
                }
            }

            // 2. If phone number is empty, fallback to Contacts URI and query Phone table by CONTACT_ID
            if (phoneNumber.isBlank()) {
                val contactCursor: Cursor? = contentResolver.query(
                    contactUri,
                    arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME),
                    null,
                    null,
                    null
                )

                var contactId: String? = null
                contactCursor?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                        val displayNameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)

                        if (idIndex != -1) {
                            contactId = cursor.getString(idIndex)
                        }
                        if (displayNameIndex != -1 && name.isBlank()) {
                            name = cursor.getString(displayNameIndex) ?: ""
                        }
                    }
                }

                if (!contactId.isNullOrEmpty()) {
                    val phonesCursor: Cursor? = contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                        "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                        arrayOf(contactId),
                        null
                    )
                    phonesCursor?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val numIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                            if (numIndex != -1) {
                                phoneNumber = cursor.getString(numIndex) ?: ""
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val cleanPhone = cleanPhoneNumber(phoneNumber)
        val cleanName = name.trim()

        if (cleanName.isEmpty() && cleanPhone.isEmpty()) {
            return null
        }

        return PickedContactInfo(
            name = cleanName,
            phoneNumber = cleanPhone
        )
    }

    /**
     * Cleans phone number by removing spaces, dashes, normalizing Arabic digits, and handling international codes.
     */
    fun cleanPhoneNumber(raw: String): String {
        var num = TimeUtils.normalizeDigits(raw)
            .replace(" ", "")
            .replace("-", "")
            .replace("(", "")
            .replace(")", "")
            .replace(".", "")
            .replace("/", "")
            .replace("،", "")
            .replace(",", "")
            .trim()
        if (num.startsWith("+20")) {
            num = "0" + num.removePrefix("+20")
        } else if (num.startsWith("0020")) {
            num = "0" + num.removePrefix("0020")
        } else if (num.startsWith("20") && num.length == 12) {
            num = "0" + num.removePrefix("20")
        }
        return num
    }
}
