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
            try {
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
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 2. If phone number is empty, fallback to Contacts URI and query Phone table by CONTACT_ID or LOOKUP_KEY
            if (phoneNumber.isBlank()) {
                var contactId: String? = null
                var lookupKey: String? = null
                try {
                    val contactCursor: Cursor? = contentResolver.query(
                        contactUri,
                        arrayOf(
                            ContactsContract.Contacts._ID,
                            ContactsContract.Contacts.LOOKUP_KEY,
                            ContactsContract.Contacts.DISPLAY_NAME
                        ),
                        null,
                        null,
                        null
                    )

                    contactCursor?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                            val lookupIndex = cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY)
                            val displayNameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)

                            if (idIndex != -1) {
                                contactId = cursor.getString(idIndex)
                            }
                            if (lookupIndex != -1) {
                                lookupKey = cursor.getString(lookupIndex)
                            }
                            if (displayNameIndex != -1 && name.isBlank()) {
                                name = cursor.getString(displayNameIndex) ?: ""
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                if (!contactId.isNullOrEmpty()) {
                    try {
                        val phonesCursor: Cursor? = contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            arrayOf(
                                ContactsContract.CommonDataKinds.Phone.NUMBER,
                                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                            ),
                            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                            arrayOf(contactId),
                            null
                        )
                        phonesCursor?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val numIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                                if (numIndex != -1) {
                                    phoneNumber = cursor.getString(numIndex) ?: ""
                                }
                                if (nameIndex != -1 && name.isBlank()) {
                                    name = cursor.getString(nameIndex) ?: ""
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                // 3. If still empty, try generic Data query with URI id
                if (phoneNumber.isBlank()) {
                    val uriLastSegment = contactUri.lastPathSegment
                    if (!uriLastSegment.isNullOrEmpty()) {
                        try {
                            val dataCursor: Cursor? = contentResolver.query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                arrayOf(
                                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                                ),
                                "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ? OR ${ContactsContract.CommonDataKinds.Phone._ID} = ?",
                                arrayOf(uriLastSegment, uriLastSegment),
                                null
                            )
                            dataCursor?.use { cursor ->
                                if (cursor.moveToFirst()) {
                                    val numIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                    val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                                    if (numIndex != -1) {
                                        phoneNumber = cursor.getString(numIndex) ?: ""
                                    }
                                    if (nameIndex != -1 && name.isBlank()) {
                                        name = cursor.getString(nameIndex) ?: ""
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
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
