package cz.jaro.alarmmorning.graphics;

import android.content.Context;
import android.util.AttributeSet;

/**
 * EditText that achieves the following:<br/>
 * 1. enabled are suggestions when the EditText has focus (and is being edited), but<br/>
 * 2. after losing focus, the proofing is disabled (so no red line appears under the incorrect words),
 * <p>
 * Source: https://stackoverflow.com/a/36479831/5726150
 */

public class EditTextWithoutSuggestion extends android.support.v7.widget.AppCompatEditText {
    public EditTextWithoutSuggestion(Context context) {
        super(context);
    }

    public EditTextWithoutSuggestion(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EditTextWithoutSuggestion(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean isSuggestionsEnabled() {
        return false;
    }
}
