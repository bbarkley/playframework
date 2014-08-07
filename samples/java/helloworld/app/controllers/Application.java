package controllers;

import com.linkedin.dataholder.DataHolder;
import play.api.libs.concurrent.ThreadLogBuffer;
import play.libs.F;
import play.libs.WS;
import play.mvc.*;
import play.data.*;
import static play.data.Form.*;
import play.data.validation.Constraints.*;

import views.html.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class Application extends Controller {
    
    /**
     * Describes the hello form.
     */
    public static class Hello {
        @Required public String name;
        @Required @Min(1) @Max(100) public Integer repeat;
        public String color;
    } 
    
    // -- Actions
  
    /**
     * Home page
     */
    public static F.Promise<Result> index() {
        DataHolder dataHolder = new DataHolder();
        DataHolder.INSTANCE.set(dataHolder);
        setCount(22);
        log("after initial set should be 22 " + buildCount());

        F.Promise<WS.Response> taResponse = WS.url("http://www.tripadvisor.com").get().map(new F.Function<WS.Response, WS.Response>() {
            @Override
            public WS.Response apply(WS.Response response) throws Throwable {
                log("In second WS map " + buildCount());
                Thread.sleep(700);
                log("Done waiting in second WS map should be 22 " + buildCount());
                setCount(44);
                log("Set in second WS map should be 44" + buildCount());
                return response;
            }
        });

        F.Promise<WS.Response> liResponse = WS.url("http://www.linkedin.com").get().map(new F.Function<WS.Response, WS.Response>() {
            @Override
            public WS.Response apply(WS.Response response) throws Throwable {
                log("In first WS map " + buildCount());
                Thread.sleep(300);
                log("Done waiting in first WS map should be 22 " + buildCount());
                setCount(33);
                log("Set in first WS map should be 33 " + buildCount());
                return response;
            }
        });

        F.Promise<String> other = F.Promise.promise(new F.Function0<String>() {
            @Override
            public String apply() throws Throwable {
                log("waiting in other future should be 22 " + buildCount());
                Thread.sleep(1500);
                log("done waiting in other future should be 22 " + buildCount());
                setCount(55);
                log("set in other future should have 55" + buildCount());
                return "FutureString!";
            }
        }).map(new F.Function<String, String>() {
            @Override
            public String apply(String s) throws Throwable {
                log("before set in other's map and should have 55 " + buildCount());
                setCount(66);
                log("set in other's map and should have 66 " + buildCount());
                return s;
            }
        });


        other.onRedeem(new F.Callback<String>() {
            @Override
            public void invoke(String s) throws Throwable {
                log("In oncomplete 1 about should have 66 " + buildCount());
                Thread.sleep(1300);
                setCount(77);
                log("In oncomplete 1 after set should be 77 and is " + buildCount());
            }
        });

        other.onRedeem(new F.Callback<String>() {
            @Override
            public void invoke(String s) throws Throwable {
                log("In oncomplete 2 about should have 66 " + buildCount());
                Thread.sleep(500);
                setCount(88);
                log("In oncomplete 2 after set should be 88 and is" + buildCount());
            }
        });

        F.Promise<List<Object>> promises = F.Promise.sequence(taResponse, liResponse, other);
        F.Promise<Result> ret = promises.map(new F.Function<List<Object>, Result>() {
            @Override
            public Result apply(List<Object> results) throws Throwable {
                WS.Response ta = (WS.Response) results.get(0);
                WS.Response li = (WS.Response) results.get(1);
                String o = (String) results.get(2);
                log("finally done and is " + buildCount());
                return ok("LI: " + li.getStatus() + " and TA: " + ta.getStatus() + " " + o);
            }
        });
        log("Outside of for block should be 22 " + buildCount());
        ret.get(10, TimeUnit.SECONDS);
        log("After await should be 22 " + buildCount());
        return ret;
    }

    public static void log(String s) {
        System.out.println(s);
        ThreadLogBuffer.log(s + "\n");
    }

    public static void setCount(int count) {
        DataHolder dataHolder = DataHolder.getInstance();
        if (dataHolder == null) {
            return;
        }
        log("setting to " + count + " for " + System.identityHashCode(dataHolder) + " on thread " + Thread.currentThread().getName());
        dataHolder.setCount(count);
    }

    public static String buildCount() {
        DataHolder dataHolder = DataHolder.getInstance();
        if (dataHolder == null) {
            return "---->Count is null for null on thread " + Thread.currentThread().getName();
        }
        return "---->Count is " + dataHolder.getCount() + " for " + System.identityHashCode(dataHolder) + " on thread " + Thread.currentThread().getName();
    }


    /**
     * Handles the form submission.
     */
    public static Result sayHello() {
        Form<Hello> form = form(Hello.class).bindFromRequest();
        if(form.hasErrors()) {
            return badRequest(index.render(form));
        } else {
            Hello data = form.get();
            return ok(
                hello.render(data.name, data.repeat, data.color)
            );
        }
    }
  
}
