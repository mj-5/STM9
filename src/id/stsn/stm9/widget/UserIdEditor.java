package id.stsn.stm9.widget;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import id.stsn.stm9.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;

public class UserIdEditor extends LinearLayout implements Editor, OnClickListener {
    private EditorListener mEditorListener = null;

    private ImageButton mDeleteButton;
    private RadioButton mIsMainUserId;
    private EditText mName;
    private EditText mEmail;

    // see http://www.regular-expressions.info/email.html
    // RFC 2822 if we omit the syntax using double quotes and square brackets
    // android.util.Patterns.EMAIL_ADDRESS is only available as of Android 2.2+
    private static final Pattern EMAIL_PATTERN = Pattern
            .compile(
                    "[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?",
                    Pattern.CASE_INSENSITIVE);

    public static class NoNameException extends Exception {
        static final long serialVersionUID = 0xf812773343L;

        public NoNameException(String message) {
            super(message);
        }
    }

    public void setCanEdit(boolean bCanEdit) {
        if (!bCanEdit) {
            mDeleteButton.setVisibility(View.INVISIBLE);
            mName.setEnabled(false);
            mIsMainUserId.setEnabled(false);
            mEmail.setEnabled(false);
        }
    }

    public static class NoEmailException extends Exception {
        static final long serialVersionUID = 0xf812773344L;

        public NoEmailException(String message) {
            super(message);
        }
    }

    public static class InvalidEmailException extends Exception {
        static final long serialVersionUID = 0xf812773345L;

        public InvalidEmailException(String message) {
            super(message);
        }
    }

    public UserIdEditor(Context context) {
        super(context);
    }

    public UserIdEditor(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        setDrawingCacheEnabled(true);
        setAlwaysDrawnWithCacheEnabled(true);

        mDeleteButton = (ImageButton) findViewById(R.id.delete);
        mDeleteButton.setOnClickListener(this);
        mIsMainUserId = (RadioButton) findViewById(R.id.isMainUserId);
        mIsMainUserId.setOnClickListener(this);

        mName = (EditText) findViewById(R.id.name);
        mEmail = (EditText) findViewById(R.id.email);

        super.onFinishInflate();
    }

    public void setValue(String userId) {
        mName.setText("");
        mEmail.setText("");

        Pattern withComment = Pattern.compile("^(.*) [(](.*)[)] <(.*)>$");
        Matcher matcher = withComment.matcher(userId);
        if (matcher.matches()) {
            mName.setText(matcher.group(1));
            mEmail.setText(matcher.group(3));
            return;
        }

        Pattern withoutComment = Pattern.compile("^(.*) <(.*)>$");
        matcher = withoutComment.matcher(userId);
        if (matcher.matches()) {
            mName.setText(matcher.group(1));
            mEmail.setText(matcher.group(2));
            return;
        }
    }

    public String getValue() throws NoNameException, NoEmailException, InvalidEmailException {
        String name = ("" + mName.getText()).trim();
        String email = ("" + mEmail.getText()).trim();

        if (email.length() > 0) {
            Matcher emailMatcher = EMAIL_PATTERN.matcher(email);
            if (!emailMatcher.matches()) {
                throw new InvalidEmailException(getContext().getString(R.string.error_invalid_email,
                        email));
            }
        }

        String userId = name;
        if (email.length() > 0) {
            userId += " <" + email + ">";
        }

        if (userId.equals("")) {
            // ok, empty one...
            return userId;
        }

        // otherwise make sure that name and email exist
        if (name.equals("")) {
            throw new NoNameException("need a name");
        }

        if (email.equals("")) {
            throw new NoEmailException("need an email");
        }

        return userId;
    }

    public void onClick(View v) {
        final ViewGroup parent = (ViewGroup) getParent();
        if (v == mDeleteButton) {
            boolean wasMainUserId = mIsMainUserId.isChecked();
            parent.removeView(this);
            if (mEditorListener != null) {
                mEditorListener.onDeleted(this);
            }
            if (wasMainUserId && parent.getChildCount() > 0) {
                UserIdEditor editor = (UserIdEditor) parent.getChildAt(0);
                editor.setIsMainUserId(true);
            }
        } else if (v == mIsMainUserId) {
            for (int i = 0; i < parent.getChildCount(); ++i) {
                UserIdEditor editor = (UserIdEditor) parent.getChildAt(i);
                if (editor == this) {
                    editor.setIsMainUserId(true);
                } else {
                    editor.setIsMainUserId(false);
                }
            }
        }
    }

    public void setIsMainUserId(boolean value) {
        mIsMainUserId.setChecked(value);
    }

    public boolean isMainUserId() {
        return mIsMainUserId.isChecked();
    }

    public void setEditorListener(EditorListener listener) {
        mEditorListener = listener;
    }
}
