package id.stsn.stm9;

public class Id {

	public static final class menu{
		public static final class opsi{
			public static final int passphrase_baru = 0x21070001;
			public static final int create = 0x21070002;
		}
	}
	
	public static final class tipe{
		public static final int user_id = 0x21070003;
		public static final int key = 0x21070004;
		public static final int secret_key = 0x21070002;
	}
	
	public static final class choice{
		public static final class algorithm {
			public static final int rsa = 0x21070003;
		}
		public static final class usage {
			public static final int sign_only = 0x21070001;
			public static final int encrypt_only = 0x21070002;
			public static final int sign_and_encrypt = 0x21070003;
		}
	}
}
