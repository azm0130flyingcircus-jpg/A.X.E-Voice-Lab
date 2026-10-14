package jp.axe.voicelab;

import android.app.Activity;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.view.Gravity;
import android.widget.*;
import android.graphics.Color;
import android.content.SharedPreferences;
import android.text.InputType;

import java.util.*;

public class MainActivity extends Activity implements TextToSpeech.OnInitListener {

    private TextToSpeech tts;
    private Spinner voiceSpinner;
    private final List<Voice> voiceList = new ArrayList<>();
    private TextView status, rateValue, pitchValue, volumeValue;
    private SeekBar rateBar, pitchBar, volumeBar;
    private EditText textInput, profileInput, commaPauseInput, periodPauseInput;
    private CheckBox splitMode;
    private SharedPreferences prefs;
    private int utteranceCounter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("axe_voice_lab", MODE_PRIVATE);
        setContentView(buildUi());
        tts = new TextToSpeech(this, this);
    }

    private int dp(int v) {
        return (int)(v * getResources().getDisplayMetrics().density);
    }

    private TextView label(String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(18);
        v.setTextColor(Color.rgb(20,20,20));
        v.setPadding(0, dp(12), 0, dp(6));
        return v;
    }

    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        return b;
    }

    private LinearLayout row() {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        return r;
    }

    private android.view.View buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(28));
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("A.X.E Voice Lab v0.2.3");
        title.setTextSize(26);
        title.setTextColor(Color.BLACK);
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("保存した声の設定を、次回も同じ位置から始める。");
        sub.setTextSize(15);
        sub.setPadding(0,0,0,dp(10));
        root.addView(sub);

        root.addView(label("AXSの返答テキスト"));
        textInput = new EditText(this);
        textInput.setMinLines(6);
        textInput.setGravity(Gravity.TOP);
        textInput.setText("なるほどね。じゃあ今日は、そっち行ってみるか。下北だったら古着屋もあるし、80年代っぽい店も探せそうだね。");
        root.addView(textInput, new LinearLayout.LayoutParams(-1, dp(180)));

        LinearLayout topButtons = row();
        Button speakTop = button("▶ 話す");
        Button stopTop = button("■ 停止");
        topButtons.addView(speakTop);
        topButtons.addView(stopTop);
        root.addView(topButtons);
        speakTop.setOnClickListener(v -> speak());
        stopTop.setOnClickListener(v -> stop());

        root.addView(label("音声"));
        voiceSpinner = new Spinner(this);
        root.addView(voiceSpinner);

        root.addView(label("保存プロファイル"));
        LinearLayout profileRow = row();
        profileInput = new EditText(this);
        profileInput.setText("A.X.E v0.1");
        Button save = button("保存");
        Button load = button("読込");
        profileRow.addView(profileInput, new LinearLayout.LayoutParams(0, -2, 1));
        profileRow.addView(save);
        profileRow.addView(load);
        root.addView(profileRow);
        save.setOnClickListener(v -> saveProfile());
        load.setOnClickListener(v -> loadProfile());

        root.addView(label("話速"));
        rateValue = new TextView(this);
        root.addView(rateValue);
        rateBar = addAdjuster(root, 60, 180, 116, 2, v -> rateValue.setText(String.format(Locale.US, "%.2f", v/100f)));

        root.addView(label("ピッチ"));
        pitchValue = new TextView(this);
        root.addView(pitchValue);
        pitchBar = addAdjuster(root, 50, 200, 128, 2, v -> pitchValue.setText(String.format(Locale.US, "%.2f", v/100f)));

        root.addView(label("音量"));
        volumeValue = new TextView(this);
        root.addView(volumeValue);
        volumeBar = addAdjuster(root, 0, 100, 100, 2, v -> volumeValue.setText(String.format(Locale.US, "%.2f", v/100f)));

        root.addView(label("読点「、」の間（ms）"));
        commaPauseInput = new EditText(this);
        commaPauseInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        commaPauseInput.setText("180");
        root.addView(commaPauseInput);
        TextView commaHint = new TextView(this);
        commaHint.setText("数字が小さいほど短く、大きいほど長い。0＝間なし / 200＝0.2秒 / 500＝0.5秒 / 1000＝1秒");
        root.addView(commaHint);

        root.addView(label("句点・感嘆符・疑問符「。！？!?」の間（ms）"));
        periodPauseInput = new EditText(this);
        periodPauseInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        periodPauseInput.setText("360");
        root.addView(periodPauseInput);
        TextView periodHint = new TextView(this);
        periodHint.setText("数字が小さいほど短く、大きいほど長い。0＝間なし / 300＝0.3秒 / 500＝0.5秒 / 1000＝1秒");
        root.addView(periodHint);

        splitMode = new CheckBox(this);
        splitMode.setText("長文を句読点ごとに分けて再生する");
        splitMode.setChecked(true);
        root.addView(splitMode);

        status = new TextView(this);
        status.setText("音声を準備中…");
        status.setPadding(0, dp(12), 0, dp(12));
        root.addView(status);

        LinearLayout bottomButtons = row();
        Button speakBottom = button("▶ 話す");
        Button stopBottom = button("■ 停止");
        bottomButtons.addView(speakBottom);
        bottomButtons.addView(stopBottom);
        root.addView(bottomButtons);
        speakBottom.setOnClickListener(v -> speak());
        stopBottom.setOnClickListener(v -> stop());

        scroll.setClipToPadding(false);
        scroll.setOnApplyWindowInsetsListener((v, insets) -> {
            int bottomInset = insets.getSystemWindowInsetBottom();
            root.setPadding(dp(20), dp(20), dp(20), bottomInset + dp(24));
            return insets;
        });
        scroll.requestApplyInsets();

        return scroll;
    }

    interface ValueListener { void onValue(int value); }

    private SeekBar addAdjuster(LinearLayout root, int min, int max, int initial, int step, ValueListener listener) {
        LinearLayout r = row();
        Button minus = button("−");
        Button plus = button("＋");
        SeekBar bar = new SeekBar(this);
        bar.setMax(max-min);
        bar.setProgress(initial-min);

        r.addView(minus);
        r.addView(bar, new LinearLayout.LayoutParams(0, -2, 1));
        r.addView(plus);
        root.addView(r);

        Runnable emit = () -> listener.onValue(bar.getProgress()+min);

        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { emit.run(); }
            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        minus.setOnClickListener(v -> {
            int value = Math.max(min, bar.getProgress()+min-step);
            bar.setProgress(value-min);
        });

        plus.setOnClickListener(v -> {
            int value = Math.min(max, bar.getProgress()+min+step);
            bar.setProgress(value-min);
        });

        emit.run();
        return bar;
    }

    @Override
    public void onInit(int code) {
        if (code != TextToSpeech.SUCCESS) {
            status.setText("音声を利用できません");
            return;
        }
        tts.setLanguage(Locale.JAPAN);
        loadVoices();
    }

    private void loadVoices() {
        voiceList.clear();
        Set<Voice> all = tts.getVoices();
        if (all != null) {
            for (Voice v : all) {
                Locale l = v.getLocale();
                if (l != null && "ja".equalsIgnoreCase(l.getLanguage())) voiceList.add(v);
            }
        }
        voiceList.sort(Comparator.comparing(Voice::getName));

        List<String> names = new ArrayList<>();
        for (Voice v : voiceList) names.add(v.getName());
        if (names.isEmpty()) names.add("日本語音声が見つかりません");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names);
        voiceSpinner.setAdapter(adapter);
        status.setText(voiceList.isEmpty() ? "日本語音声が見つかりません" : "準備完了");
    }

    private int intValue(EditText e, int fallback) {
        try { return Integer.parseInt(e.getText().toString().trim()); }
        catch (Exception ex) { return fallback; }
    }

    private void applyVoiceSettings() {
        if (!voiceList.isEmpty()) {
            int pos = voiceSpinner.getSelectedItemPosition();
            if (pos >= 0 && pos < voiceList.size()) tts.setVoice(voiceList.get(pos));
        }
        tts.setSpeechRate((rateBar.getProgress()+60)/100f);
        tts.setPitch((pitchBar.getProgress()+50)/100f);
    }

    private Bundle volumeParams() {
        Bundle b = new Bundle();
        b.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volumeBar.getProgress()/100f);
        return b;
    }

    private void speak() {
        if (tts == null) return;
        tts.stop();
        applyVoiceSettings();

        String text = textInput.getText().toString();
        if (text.trim().isEmpty()) return;

        if (!splitMode.isChecked()) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, volumeParams(), "u" + (++utteranceCounter));
            status.setText("再生中");
            return;
        }

        int comma = intValue(commaPauseInput,180);
        int period = intValue(periodPauseInput,360);
        List<Object[]> parts = splitText(text, comma, period);

        boolean first = true;
        for (Object[] part : parts) {
            String segment = (String)part[0];
            int pause = (Integer)part[1];
            if (!segment.isEmpty()) {
                tts.speak(segment, first ? TextToSpeech.QUEUE_FLUSH : TextToSpeech.QUEUE_ADD, volumeParams(), "u" + (++utteranceCounter));
                first = false;
            }
            if (pause > 0) tts.playSilentUtterance(pause, TextToSpeech.QUEUE_ADD, "p" + (++utteranceCounter));
        }
        status.setText("再生中");
    }

    private List<Object[]> splitText(String text, int commaPause, int periodPause) {
        List<Object[]> out = new ArrayList<>();
        StringBuilder buf = new StringBuilder();

        for (int i=0; i<text.length(); i++) {
            char c = text.charAt(i);
            buf.append(c);

            if (c=='、') {
                out.add(new Object[]{buf.toString(), commaPause});
                buf.setLength(0);
            } else if (c=='。' || c=='！' || c=='？' || c=='!' || c=='?') {
                out.add(new Object[]{buf.toString(), periodPause});
                buf.setLength(0);
            }
        }

        if (buf.length() > 0) out.add(new Object[]{buf.toString(), 0});
        return out;
    }

    private void stop() {
        if (tts != null) tts.stop();
        status.setText("停止しました");
    }

    private void saveProfile() {
        String key = profileInput.getText().toString().trim();
        if (key.isEmpty()) key = "A.X.E v0.1";

        SharedPreferences.Editor e = prefs.edit();
        e.putString(key+".voice", voiceSpinner.getSelectedItem()==null ? "" : voiceSpinner.getSelectedItem().toString());
        e.putInt(key+".rate", rateBar.getProgress());
        e.putInt(key+".pitch", pitchBar.getProgress());
        e.putInt(key+".volume", volumeBar.getProgress());
        e.putInt(key+".comma", intValue(commaPauseInput,180));
        e.putInt(key+".period", intValue(periodPauseInput,360));
        e.putBoolean(key+".split", splitMode.isChecked());
        e.apply();

        status.setText("保存しました：" + key);
    }

    private void loadProfile() {
        String key = profileInput.getText().toString().trim();
        if (key.isEmpty()) key = "A.X.E v0.1";

        if (!prefs.contains(key+".rate")) {
            status.setText("この名前の保存データはありません");
            return;
        }

        rateBar.setProgress(prefs.getInt(key+".rate",56));
        pitchBar.setProgress(prefs.getInt(key+".pitch",78));
        volumeBar.setProgress(prefs.getInt(key+".volume",100));
        commaPauseInput.setText(String.valueOf(prefs.getInt(key+".comma",180)));
        periodPauseInput.setText(String.valueOf(prefs.getInt(key+".period",360)));
        splitMode.setChecked(prefs.getBoolean(key+".split",true));

        String voiceName = prefs.getString(key+".voice","");
        for (int i=0; i<voiceSpinner.getCount(); i++) {
            if (voiceSpinner.getItemAtPosition(i).toString().equals(voiceName)) {
                voiceSpinner.setSelection(i);
                break;
            }
        }

        status.setText("読み込みました：" + key);
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
