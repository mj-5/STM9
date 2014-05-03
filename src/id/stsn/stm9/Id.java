package id.stsn.stm9;

import org.spongycastle.bcpg.CompressionAlgorithmTags;

public final class Id {

    public static final String TAG = "APG";

    public static final class menu {
        public static final int export = 0x21070001;
        public static final int exportToServer = 0x21070002;

        public static final class opsi {
            public static final int create = 0x21070001;
            public static final int export_keys = 0x21070002;
            public static final int key_server = 0x21070003;
            public static final int encrypt = 0x21070004;
            public static final int decrypt = 0x21070005;
        }
    }

    public static final class request {
        public static final int public_keys = 0x00007001;
        public static final int secret_keys = 0x00007002;
        public static final int filename = 0x00007003;
        public static final int look_up_key_id = 0x00007004;
        public static final int export_to_server = 0x00007005;
    }

    public static final class tipe {
        public static final int public_key = 0x21070001;
        public static final int secret_key = 0x21070002;
        public static final int user_id = 0x21070003;
        public static final int key = 0x21070004;
    }

    public static final class pilihan {
        public static final class algoritma {
            public static final int rsa = 0x21070001;
        }

        public static final class compression {
            public static final int none = 0x21070001;
            public static final int zip = CompressionAlgorithmTags.ZIP;
        }

        public static final class usage {
            public static final int sign_only = 0x21070001;
            public static final int encrypt_only = 0x21070002;
            public static final int sign_and_encrypt = 0x21070003;
        }

    }

    public static final class return_value {
        public static final int ok = 0;
        public static final int error = -1;
        public static final int updated = 1;
        public static final int bad = -2;
    }

    public static final class target {
        public static final int email = 0x21070001;
        public static final int message = 0x21070002;
    }

    public static final class kunci {
        public static final int none = 0;
        public static final int symmetric = -1;
    }


    public static final class keyserver {
        public static final int search = 0x21070001;
        public static final int get = 0x21070002;
    }
}
