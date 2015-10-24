package org.usc.check.in;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class AppMain {

    @SuppressWarnings("resource")
    public static void main(String[] args) throws Exception {
        new AnnotationConfigApplicationContext(AppConfig.class);

        // AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
        // context.getBean(SmzdmAndroidCheckInTask.class).run();
        // context.getBean(V2exCheckInTask.class).run();
        // context.getBean(ZiMuZuTvSignInTask.class).run();
        // context.getBean(MyPhotoCheckInTask.class).run();
        // context.getBean(MoliWebCheckInTask.class).run();
        // context.getBean(LitsCloudCheckInTask.class).run();
        // context.getBean(MiaoCheckInTask.class).run();
        //
        // System.out.println("end");
    }
}
