package id.stsn.stm9.utility;

public class Choice {
  private String mName;
  private int mId;

  public Choice() {
      mId = -1;
      mName = "";
  }

  public Choice(int id, String name) {
      mId = id;
      mName = name;
  }

  public int getId() {
      return mId;
  }

  public String getName() {
      return mName;
  }

  @Override
	public String toString() {
      return mName;
  }
}
