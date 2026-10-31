package ec.mxq.inicio;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** A small Android 7.1 home. Usage history stays on the device. */
public class MainActivity extends Activity {
    private static final int WHITE = Color.rgb(241, 246, 249);
    private static final int MUTED = Color.rgb(151, 170, 186);
    private static final int MINT = Color.rgb(155, 232, 207);
    private static final Locale ES = new Locale("es", "EC");
    private static final String[] CONTENT = {"org.smarttube.stable", "com.google.android.youtube.tv", "org.xbmc.kodi"};
    private final Handler handler = new Handler();
    private final ExecutorService network = Executors.newFixedThreadPool(2);
    private TextView clock, date, weather, weatherMeta;
    private LinearLayout recentRow;
    private GridLayout appsGrid;
    private TextView appsTitle;
    private SharedPreferences preferences;
    private List<App> apps = new ArrayList<App>();
    private boolean resumed, weatherBusy;
    private int screenWidth;
    private long appsLoadedAt;
    private int refreshGeneration;
    private String recentOrder = "";
    private final Map<String,TextView> recentCaptions = new HashMap<String,TextView>();

    private static class App {
        String packageName, label; Drawable icon;
        Intent intent;
        App(String p, String l, Intent i) { packageName = p; label = l; intent = i; }
    }

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        preferences = getSharedPreferences("home", MODE_PRIVATE);
        screenWidth = (int)(getResources().getDisplayMetrics().widthPixels / getResources().getDisplayMetrics().density);
        buildHome();
    }

    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private LinearLayout vertical() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private TextView text(String value, float size, int color) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        t.setFontFeatureSettings("kern"); t.setIncludeFontPadding(false); return t;
    }
    private GradientDrawable surface(int top, int bottom, int border) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{top, bottom});
        d.setCornerRadius(dp(18)); if (border != 0) d.setStroke(dp(1), border); return d;
    }
    private void margin(View v, int top, int bottom) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.topMargin = dp(top); lp.bottomMargin = dp(bottom); v.setLayoutParams(lp);
    }

    private void buildHome() {
        FrameLayout backdrop = new FrameLayout(this);
        backdrop.addView(new View(this) {
            private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            @Override protected void onDraw(Canvas c) {
                c.drawColor(Color.rgb(9, 17, 27));
                paint.setShader(new RadialGradient(getWidth() * .78f, getHeight() * .05f, getWidth() * .65f,
                    new int[]{Color.rgb(29, 64, 72), Color.rgb(9, 17, 27)}, null, Shader.TileMode.CLAMP));
                c.drawRect(0, 0, getWidth(), getHeight(), paint);
                paint.setShader(null); paint.setColor(0x073FE0C2); c.drawCircle(getWidth()*.93f, getHeight()*.2f, dp(220), paint);
            }
        }, new FrameLayout.LayoutParams(-1, -1));
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setClipToPadding(false);
        scroll.setVerticalScrollBarEnabled(false); scroll.setClipChildren(false);
        LinearLayout body = vertical(); body.setPadding(dp(46), dp(25), dp(46), dp(20));
        body.setClipChildren(false); body.setClipToPadding(false);
        scroll.addView(body, new ScrollView.LayoutParams(-1, -2));
        backdrop.addView(scroll, new FrameLayout.LayoutParams(-1, -1)); setContentView(backdrop);

        LinearLayout header = new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL);
        body.addView(header, new LinearLayout.LayoutParams(-1, dp(107)));
        LinearLayout brand = vertical();
        TextView tag = text("MXQ  /  INICIO", 13, MINT); tag.setLetterSpacing(.18f); brand.addView(tag);
        TextView greeting = text("Tu espacio, sin ruido.", 31, WHITE); greeting.setTypeface(Typeface.create("sans-serif-light", 0)); margin(greeting, 9, 0); brand.addView(greeting);
        header.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout time = vertical(); time.setGravity(Gravity.RIGHT);
        clock = text("", 43, WHITE); clock.setTypeface(Typeface.create("sans-serif-light", 0)); time.addView(clock);
        date = text("", 13, MUTED); margin(date, 5, 0); time.addView(date);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(dp(210), -2); tlp.rightMargin=dp(27); header.addView(time, tlp);

        LinearLayout climate = vertical(); climate.setPadding(dp(19), dp(15), dp(19), dp(15));
        climate.setGravity(Gravity.CENTER_VERTICAL);
        TextView location = text("AMBATO, ECUADOR", 11, MINT); location.setLetterSpacing(.1f); climate.addView(location);
        weather = text("Consultando clima…", 19, WHITE); weather.setMaxLines(1); weather.setEllipsize(TextUtils.TruncateAt.END); margin(weather, 7, 0); climate.addView(weather);
        weatherMeta = text("Open-Meteo · sin ubicación GPS", 10, MUTED); margin(weatherMeta, 6, 0); climate.addView(weatherMeta);
        header.addView(climate, new LinearLayout.LayoutParams(dp(274), dp(107)));
        focusable(climate, Color.rgb(26, 49, 59), Color.rgb(19, 37, 48));
        climate.setOnClickListener(v -> fetchWeather(true));

        TextView intro = text("CONTINUAR / RECIENTES", 12, MINT); intro.setLetterSpacing(.12f); margin(intro, 23, 5); body.addView(intro);
        TextView note = text("Vuelve a tus apps de contenido. El progreso se conserva en cada app.", 13, MUTED); margin(note, 0, 13); body.addView(note);
        recentRow = new LinearLayout(this); recentRow.setClipChildren(false); recentRow.setClipToPadding(false);
        body.addView(recentRow, new LinearLayout.LayoutParams(-1, dp(133)));

        appsTitle = text("TUS APLICACIONES", 12, MUTED); appsTitle.setLetterSpacing(.12f); margin(appsTitle, 24, 12); body.addView(appsTitle);
        appsGrid = new GridLayout(this); appsGrid.setColumnCount(6); appsGrid.setClipChildren(false); appsGrid.setClipToPadding(false);
        body.addView(appsGrid, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout footer = new LinearLayout(this); footer.setGravity(Gravity.CENTER_VERTICAL); margin(footer, 16, 0); body.addView(footer);
        TextView hint = text("D-PAD  Mover     OK  Abrir     HOME  Volver aquí", 11, MUTED);
        footer.addView(hint, new LinearLayout.LayoutParams(0, -2, 1));
        footer.addView(action("Ajustes", () -> openSettings()));
        footer.addView(action("Menú original", () -> openFactory()));
        footer.addView(action("Acerca de", () -> about()));
        renderWeatherCache(); tick.run();
    }

    private TextView action(String label, Runnable onClick) {
        TextView v = text(label, 12, WHITE); v.setGravity(Gravity.CENTER); v.setPadding(dp(15), dp(11), dp(15), dp(11));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2); lp.leftMargin=dp(8); v.setLayoutParams(lp);
        focusable(v, Color.rgb(26, 42, 55), Color.rgb(21, 34, 46)); v.setOnClickListener(w -> onClick.run()); return v;
    }

    private void focusable(final View v, final int top, final int bottom) {
        v.setFocusable(true); v.setClickable(true);
        StateListDrawable background = new StateListDrawable();
        background.addState(new int[]{android.R.attr.state_focused},surface(top,bottom,MINT));
        background.addState(new int[]{},surface(top,bottom,0x22B8D7DD));
        v.setBackground(background);
        v.setOnFocusChangeListener((view, focused) -> {
            view.animate().scaleX(focused ? 1.025f : 1f).scaleY(focused ? 1.025f : 1f)
                .setDuration(110).setInterpolator(new DecelerateInterpolator()).withLayer().start();
        });
    }

    private List<App> installedApps() {
        PackageManager pm = getPackageManager(); Map<String, App> found = new LinkedHashMap<String, App>();
        for (String category : new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}) {
            Intent query = new Intent(Intent.ACTION_MAIN).addCategory(category);
            for (ResolveInfo r : pm.queryIntentActivities(query, 0)) {
                String p = r.activityInfo.packageName;
                if (p.equals(getPackageName()) || p.equals("com.droidlogic.mboxlauncher") || found.containsKey(p)) continue;
                Intent launch = pm.getLeanbackLaunchIntentForPackage(p);
                if (launch == null) launch = pm.getLaunchIntentForPackage(p);
                if (launch != null) {
                    App app=new App(p,r.loadLabel(pm).toString(),launch);
                    try{app.icon=pm.getApplicationIcon(p);}catch(PackageManager.NameNotFoundException ignored){}
                    found.put(p,app);
                }
            }
        }
        List<App> list = new ArrayList<App>(found.values());
        Collections.sort(list, (a,b) -> {
            int ar = rank(a.packageName), br = rank(b.packageName);
            return ar == br ? a.label.compareToIgnoreCase(b.label) : Integer.compare(ar, br);
        }); return list;
    }
    private int rank(String p) { for (int i=0; i<CONTENT.length; i++) if (p.equals(CONTENT[i])) return i; return 10; }
    private App findApp(String p) { for (App a : apps) if (a.packageName.equals(p)) return a; return null; }
    private Map<String, Long> lastUsed() {
        Map<String, Long> times = new HashMap<String, Long>();
        for (String p : CONTENT) times.put(p, preferences.getLong("used_"+p, 0L));
        try {
            UsageStatsManager usage = (UsageStatsManager)getSystemService(USAGE_STATS_SERVICE);
            long now = System.currentTimeMillis();
            List<UsageStats> records = usage.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now-30L*86400000L, now);
            if (records != null) for (UsageStats record : records) {
                String p = record.getPackageName();
                if (times.containsKey(p)) times.put(p, Math.max(times.get(p), record.getLastTimeUsed()));
            }
        } catch (RuntimeException ignored) { /* Local click history remains available. */ }
        return times;
    }

    private void refreshApps() {
        final int generation=++refreshGeneration;
        final boolean reload=apps.isEmpty()||System.currentTimeMillis()-appsLoadedAt>60000;
        network.execute(()->{
            final List<App> loaded=reload?installedApps():null;
            final Map<String,Long> times=lastUsed();
            handler.post(()->{
                if(!resumed||isFinishing()||isDestroyed()||generation!=refreshGeneration)return;
                if(loaded!=null){apps=loaded;appsLoadedAt=System.currentTimeMillis();}
                populateApps(times,loaded!=null);
            });
        });
    }
    private Drawable iconCopy(App a) {
        if(a.icon==null)return null;
        return a.icon.getConstantState()==null?a.icon:a.icon.getConstantState().newDrawable(getResources());
    }
    private void populateApps(final Map<String,Long> times,boolean rebuildGrid) {
        List<App> recent = new ArrayList<App>();
        for (String p : CONTENT) { App a = findApp(p); if (a != null) recent.add(a); }
        Collections.sort(recent, (a,b) -> Long.compare(times.get(b.packageName), times.get(a.packageName)));
        int n = recent.size();
        StringBuilder order=new StringBuilder();for(App a:recent)order.append(a.packageName).append(';');
        boolean rebuildRecent=!order.toString().equals(recentOrder)||rebuildGrid;
        if(rebuildRecent){recentRow.removeAllViews();recentCaptions.clear();recentOrder=order.toString();}
        for (int i=0; i<n; i++) {
            App a = recent.get(i);
            if(!rebuildRecent){recentCaptions.get(a.packageName).setText(recentStatus(times.get(a.packageName)));continue;}
            View card = contentCard(a, times.get(a.packageName));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1); if (i<n-1) lp.rightMargin=dp(14);
            recentRow.addView(card, lp);
        }
        if (n == 0&&rebuildRecent) recentRow.addView(text("Instala SmartTube, Xuper o Kodi para empezar.", 19, WHITE));
        appsTitle.setText("TUS APLICACIONES  /  " + apps.size());
        int col=6, width=(screenWidth-92-5*12)/col;
        if(rebuildGrid)appsGrid.removeAllViews();
        for (int i=0; rebuildGrid&&i<apps.size(); i++) {
            App a=apps.get(i); LinearLayout tile=vertical(); tile.setGravity(Gravity.CENTER); tile.setPadding(dp(10),dp(11),dp(10),dp(10));
            ImageView icon = new ImageView(this); icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            icon.setImageDrawable(iconCopy(a));
            tile.addView(icon, new LinearLayout.LayoutParams(dp(37),dp(37)));
            TextView label=text(a.label, 12, WHITE); label.setMaxLines(1); label.setEllipsize(TextUtils.TruncateAt.END); label.setGravity(Gravity.CENTER); margin(label,8,0); tile.addView(label);
            focusable(tile,Color.rgb(26,40,54),Color.rgb(19,31,44)); tile.setOnClickListener(v -> launch(a)); tile.setContentDescription(a.label);
            GridLayout.LayoutParams lp=new GridLayout.LayoutParams(GridLayout.spec(i/col),GridLayout.spec(i%col)); lp.width=dp(width); lp.height=dp(93);
            lp.rightMargin=dp(i%col<col-1 ? 12 : 0); lp.bottomMargin=dp(12); appsGrid.addView(tile,lp);
        }
        // Land on content when HOME is pressed; Android handles D-pad focus and scroll.
        if (recentRow.getChildCount()>0) recentRow.getChildAt(0).requestFocus();
    }

    private View contentCard(final App a, long last) {
        int top=Color.rgb(36,56,75), bottom=Color.rgb(24,36,54); String kind="BIBLIOTECA";
        if (a.packageName.equals("org.smarttube.stable")) { top=Color.rgb(73,42,49); bottom=Color.rgb(42,30,43); kind="VIDEOS Y SUSCRIPCIONES"; }
        if (a.packageName.equals("com.google.android.youtube.tv")) { top=Color.rgb(72,40,42); bottom=Color.rgb(43,28,35); kind="YOUTUBE PARA TV"; }
        if (a.packageName.equals("com.android.mgstv")) { top=Color.rgb(39,49,78); bottom=Color.rgb(26,33,54); kind="CANALES Y CONTENIDO"; }
        if (a.packageName.equals("org.xbmc.kodi")) { top=Color.rgb(25,63,76); bottom=Color.rgb(20,40,55); kind="TU CENTRO MULTIMEDIA"; }
        LinearLayout card=new LinearLayout(this); card.setGravity(Gravity.CENTER_VERTICAL); card.setPadding(dp(23),dp(18),dp(21),dp(18));
        LinearLayout labels=vertical();
        TextView eyebrow=text(kind,9,MUTED); eyebrow.setLetterSpacing(.09f); labels.addView(eyebrow);
        TextView name=text(a.label,27,WHITE); name.setTypeface(Typeface.create("sans-serif-medium",0)); margin(name,9,0); labels.addView(name);
        String status=recentStatus(last);
        TextView caption=text(status,12,MINT); margin(caption,10,0); labels.addView(caption);
        recentCaptions.put(a.packageName,caption);
        card.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
        ImageView icon=new ImageView(this); icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        icon.setImageDrawable(iconCopy(a));
        card.addView(icon,new LinearLayout.LayoutParams(dp(58),dp(58)));
        focusable(card,top,bottom); card.setOnClickListener(v -> launch(a)); card.setContentDescription("Abrir "+a.label); return card;
    }
    private String recentStatus(long last){return last>0 ? "Usada "+ago(last)+"   ›" : "Abrir aplicación   ›";}
    private String ago(long timestamp) {
        long minutes=Math.max(0,(System.currentTimeMillis()-timestamp)/60000);
        if(minutes<1)return "ahora"; if(minutes<60)return "hace "+minutes+" min";
        if(minutes<1440)return "hace "+minutes/60+" h"; return "hace "+minutes/1440+" días";
    }
    private void launch(App a) {
        try { startActivity(a.intent); preferences.edit().putLong("used_"+a.packageName,System.currentTimeMillis()).apply(); }
        catch(RuntimeException ex){ Toast.makeText(this,"No se pudo abrir "+a.label,Toast.LENGTH_LONG).show(); }
    }
    private void openSettings() { try { startActivity(new Intent(Settings.ACTION_SETTINGS)); } catch (RuntimeException e) { Toast.makeText(this,"Ajustes no disponibles",0).show(); } }
    private void openFactory() {
        try { startActivity(new Intent(Intent.ACTION_MAIN).setComponent(new ComponentName("com.droidlogic.mboxlauncher","com.droidlogic.mboxlauncher.Launcher"))); }
        catch(RuntimeException e){ Toast.makeText(this,"Menú original no disponible",0).show(); }
    }
    private void about() {
        new AlertDialog.Builder(this).setTitle("MXQ Inicio 1.0")
            .setMessage("Un inicio ligero para tu TV Box.\n\nRecientes ordena SmartTube, YouTube y Kodi según su uso. El punto exacto de reproducción depende de cada aplicación.\n\nClima de Ambato: Open-Meteo (CC BY 4.0), actualizado cada 15 minutos mientras el menú está abierto. No usa GPS.\n\nEl historial de uso permanece en este dispositivo.")
            .setPositiveButton("Entendido",null).setNeutralButton("Permiso de recientes",(d,w)->{
                try{startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));}catch(RuntimeException ignored){}
            }).show();
    }

    private final Runnable tick=new Runnable(){ public void run(){
        SimpleDateFormat tf=new SimpleDateFormat("HH:mm",ES), df=new SimpleDateFormat("EEEE d 'de' MMMM",ES);
        TimeZone zone=TimeZone.getTimeZone("America/Guayaquil"); tf.setTimeZone(zone); df.setTimeZone(zone);
        clock.setText(tf.format(new Date())); date.setText(df.format(new Date()));
        if(resumed)handler.postDelayed(this,10000);
    }};
    private final Runnable weatherTick=new Runnable(){public void run(){fetchWeather(false);if(resumed)handler.postDelayed(this,900000);}};

    private String weatherName(int code) {
        if(code==0)return "Despejado"; if(code==1)return "Poco nublado"; if(code==2)return "Parc. nublado"; if(code==3)return "Nublado";
        if(code==45||code==48)return "Niebla"; if(code>=51&&code<=57)return "Llovizna";
        if(code>=61&&code<=67)return "Lluvia"; if(code>=71&&code<=77)return "Nieve";
        if(code>=80&&code<=82)return "Chubascos"; if(code>=85&&code<=86)return "Nieve";
        if(code>=95)return "Tormenta"; return "Condiciones actuales";
    }
    private void renderWeatherCache() {
        String value=preferences.getString("weather_value",null);
        long stamp=preferences.getLong("weather_at",0);
        if(value!=null){weather.setText(value); weatherMeta.setText("Open-Meteo · "+(System.currentTimeMillis()-stamp>1800000?"dato anterior":"actualizado "+ago(stamp)));}
    }
    private void fetchWeather(boolean force) {
        if(weatherBusy)return;
        if(!force&&System.currentTimeMillis()-preferences.getLong("weather_at",0)<900000){renderWeatherCache();return;}
        weatherBusy=true;
        network.execute(()->{
            HttpURLConnection connection=null;
            try{
                URL url=new URL("https://api.open-meteo.com/v1/forecast?latitude=-1.24908&longitude=-78.61675&current=temperature_2m,weather_code&timezone=America%2FGuayaquil&forecast_days=1");
                connection=(HttpURLConnection)url.openConnection(); connection.setConnectTimeout(10000); connection.setReadTimeout(10000);
                connection.setRequestProperty("User-Agent","MXQ-Inicio/1.0");
                if(connection.getResponseCode()!=200)throw new java.io.IOException("Weather HTTP "+connection.getResponseCode());
                StringBuilder json=new StringBuilder();
                try(BufferedReader reader=new BufferedReader(new InputStreamReader(connection.getInputStream(),"UTF-8"))){String line;while((line=reader.readLine())!=null){json.append(line);if(json.length()>100000)throw new java.io.IOException("Response too large");}}
                JSONObject current=new JSONObject(json.toString()).getJSONObject("current");
                String value=Math.round(current.getDouble("temperature_2m"))+"°  ·  "+weatherName(current.getInt("weather_code"));
                preferences.edit().putString("weather_value",value).putLong("weather_at",System.currentTimeMillis()).apply();
                handler.post(()->{if(!isFinishing()&&!isDestroyed())renderWeatherCache();});
            }catch(Exception error){
                android.util.Log.w("MXQWeather","Weather unavailable: "+error.getClass().getSimpleName());
                handler.post(()->{if(isFinishing()||isDestroyed())return;renderWeatherCache();if(!preferences.contains("weather_value"))weather.setText("Clima no disponible");weatherMeta.setText("Open-Meteo · sin conexión / OK reintenta");});
            }finally{if(connection!=null)connection.disconnect();handler.post(()->weatherBusy=false);}
        });
    }
    @Override protected void onResume(){super.onResume();resumed=true;handler.removeCallbacks(tick);tick.run();refreshApps();handler.removeCallbacks(weatherTick);weatherTick.run();}
    @Override protected void onPause(){resumed=false;handler.removeCallbacks(tick);handler.removeCallbacks(weatherTick);super.onPause();}
    @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);network.shutdownNow();super.onDestroy();}
    @Override public void onBackPressed(){if(recentRow.getChildCount()>0)recentRow.getChildAt(0).requestFocus();}
}
