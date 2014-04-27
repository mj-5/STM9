package id.stsn.stm9.widget;

public interface Editor {
    public interface EditorListener {
        public void onDeleted(Editor editor);
    }

    public void setEditorListener(EditorListener listener);
}
