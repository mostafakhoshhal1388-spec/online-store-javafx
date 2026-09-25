package ir.store.admin;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import javafx.application.Application;
import javafx.collections.*;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class AdminApp extends Application {
    private static final String API = System.getenv().getOrDefault("STORE_API", "http://localhost:8080/api/products");
    private static final String ADMIN_KEY = System.getenv().getOrDefault("STORE_ADMIN_KEY", "local-dev-key");
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObservableList<String> products = FXCollections.observableArrayList();
    private final ListView<String> list = new ListView<>(products);
    private final TextField name = new TextField(), price = new TextField(), stock = new TextField(), image = new TextField();
    private final TextArea description = new TextArea();
    private Long selectedId;

    @Override public void start(Stage stage) {
        stage.setTitle("مدیریت فروشگاه");
        name.setPromptText("نام کالا"); price.setPromptText("قیمت به تومان"); stock.setPromptText("موجودی"); image.setPromptText("نشانی تصویر (اختیاری)"); description.setPromptText("توضیحات"); description.setPrefRowCount(3);
        list.setPrefWidth(360);
        list.setCellFactory(v -> new ListCell<>() { @Override protected void updateItem(String item, boolean empty) { super.updateItem(item,empty); setText(empty?null:item); } });
        list.getSelectionModel().selectedIndexProperty().addListener((o,a,b)->{if(b.intValue()>=0) select(b.intValue());});
        Button save=new Button("ذخیره کالا"), fresh=new Button("کالای تازه"), delete=new Button("حذف کالای انتخاب‌شده"), refresh=new Button("به‌روزرسانی فهرست");
        save.setOnAction(e->save()); fresh.setOnAction(e->clear()); delete.setOnAction(e->remove()); refresh.setOnAction(e->load());
        VBox form=new VBox(12,new Label("جزئیات کالا"),name,price,stock,image,description,new HBox(10,save,fresh),delete);
        form.setPadding(new Insets(18)); form.setPrefWidth(390);
        BorderPane root=new BorderPane(); root.setTop(new HBox(14,new Label("مدیریت فروشگاه"),refresh)); root.setLeft(list); root.setCenter(form); BorderPane.setMargin(root.getTop(),new Insets(18));
        stage.setScene(new Scene(root,800,520)); stage.show(); load();
    }
    private String request(String method,String body){try{var b=HttpRequest.newBuilder(URI.create(API)).header("Content-Type","application/json"); if(method.equals("GET"))b.GET();else b.method(method,HttpRequest.BodyPublishers.ofString(body==null?"":body,StandardCharsets.UTF_8));var r=http.send(b.build(),HttpResponse.BodyHandlers.ofString());if(r.statusCode()>=400)throw new IllegalStateException("خطای سرور: "+r.statusCode()+" "+r.body());return r.body();}catch(Exception ex){throw new RuntimeException(ex.getMessage(),ex);}}
    private void load(){try{products.clear();String json=request("GET",null);var matcher=java.util.regex.Pattern.compile("\\{[^{}]*}").matcher(json);while(matcher.find()){String obj=matcher.group();long id=num(obj,"id");String n=str(obj,"name");products.add(id+"|"+n+"|"+num(obj,"price")+"|"+num(obj,"stock")+"|"+str(obj,"description")+"|"+str(obj,"imageUrl"));}}catch(Exception e){alert(e.getMessage());}}
    private long num(String json,String key){var m=java.util.regex.Pattern.compile("\\\""+key+"\\\"\\s*:\\s*(\\d+)").matcher(json);return m.find()?Long.parseLong(m.group(1)):0;}
    private String str(String json,String key){var m=java.util.regex.Pattern.compile("\\\""+key+"\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"").matcher(json);return m.find()?m.group(1):"";}
    private void select(int i){String[] p=products.get(i).split("\\|",-1);selectedId=Long.parseLong(p[0]);name.setText(p[1]);price.setText(p[2]);stock.setText(p[3]);description.setText(p.length>4?p[4]:"");image.setText(p.length>5?p[5]:"");}
    private void save(){try{String body="{\"name\":"+q(name.getText())+",\"description\":"+q(description.getText())+",\"price\":"+Double.parseDouble(price.getText())+",\"stock\":"+Integer.parseInt(stock.getText())+",\"imageUrl\":"+q(image.getText())+"}";String endpoint=selectedId==null?API:API+"/"+selectedId;String method=selectedId==null?"POST":"PUT";HttpRequest req=HttpRequest.newBuilder(URI.create(endpoint)).header("Content-Type","application/json").header("X-Admin-Key",ADMIN_KEY).method(method,HttpRequest.BodyPublishers.ofString(body,StandardCharsets.UTF_8)).build();var response=http.send(req,HttpResponse.BodyHandlers.ofString());if(response.statusCode()>=400)throw new IllegalStateException("خطای سرور: "+response.statusCode()+" "+response.body());clear();load();}catch(Exception e){alert(e.getMessage());}}
    private String q(String s){return "\""+s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n")+"\"";}
    private void remove(){if(selectedId==null)return;try{var response=http.send(HttpRequest.newBuilder(URI.create(API+"/"+selectedId)).header("X-Admin-Key",ADMIN_KEY).DELETE().build(),HttpResponse.BodyHandlers.ofString());if(response.statusCode()>=400)throw new IllegalStateException("خطای سرور: "+response.statusCode()+" "+response.body());clear();load();}catch(Exception e){alert(e.getMessage());}}
    private void clear(){selectedId=null;name.clear();price.clear();stock.clear();description.clear();image.clear();list.getSelectionModel().clearSelection();}
    private void alert(String message){new Alert(Alert.AlertType.ERROR,message==null?"خطای نامشخص":message,ButtonType.OK).showAndWait();}
    public static void main(String[] args){launch(args);}
}
