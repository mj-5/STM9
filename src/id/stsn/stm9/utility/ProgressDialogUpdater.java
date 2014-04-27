package id.stsn.stm9.utility;

public interface ProgressDialogUpdater {
    void setProgress(String message, int current, int total);

    void setProgress(int resourceId, int current, int total);

    void setProgress(int current, int total);
}
