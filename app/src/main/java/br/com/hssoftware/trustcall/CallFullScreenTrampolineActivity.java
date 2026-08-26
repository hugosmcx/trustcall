package br.com.hssoftware.trustcall;

import android.app.Activity;
import android.os.Bundle;

public class CallFullScreenTrampolineActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        finish();
    }
}
