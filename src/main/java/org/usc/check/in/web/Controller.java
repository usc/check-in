package org.usc.check.in.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.usc.check.in.task.SmzdmAndroidTask;
import org.usc.check.in.task.V2exCheckInTask;
import org.usc.check.in.task.ZiMuZuTvSignInTask;

@RestController
@RequestMapping("/check/in")
public class Controller {
    @Autowired
    private SmzdmAndroidTask smzdmAndroidTask;
    @Autowired
    private V2exCheckInTask v2exCheckInTask;
    @Autowired
    private ZiMuZuTvSignInTask ziMuZuTvSignInTask;

    @RequestMapping(value = "/smzdm", method = RequestMethod.GET)
    public String smzdm() {
        smzdmAndroidTask.run();

        return "success";
    }

    @RequestMapping(value = "/v2ex", method = RequestMethod.GET)
    public String v2ex() {
        v2exCheckInTask.run();

        return "success";
    }

    @RequestMapping(value = "/zimuzu", method = RequestMethod.GET)
    public String ziMuZu() {
        ziMuZuTvSignInTask.run();

        return "success";
    }

}
