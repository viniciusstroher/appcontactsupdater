package br.rcx.updatercontacts.persistence;

import android.provider.BaseColumns;

public final class PhonesContract {
    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor private.
    private PhonesContract() {}

    /* Inner class that defines the table contents */
    public static class PhonesEntry implements BaseColumns {
        public static final String TABLE_NAME = "phones";
        public static final String COLUMN_PHONE = "phone";
        public static final String COLUMN_CONTACT_ID = "contact_id";
        public static final String COLUMN_HAS_WHATS = "has_whats";
        public static final String COLUMN_CREATED_AT = "created_at";
        public static final String COLUMN_PROCESS_AT = "process_at";
    }
}
