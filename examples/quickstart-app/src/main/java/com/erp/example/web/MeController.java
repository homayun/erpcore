package com.erp.example.web;

import com.erp.core.security.api.actor.CurrentActor;
import com.erp.core.security.api.actor.CurrentActorProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MeController {

    private final CurrentActorProvider actorProvider;

    public MeController(CurrentActorProvider actorProvider) {
        this.actorProvider = actorProvider;
    }

    @GetMapping("/me")
    public CurrentActor me() {
        return actorProvider.getCurrentActor();
    }
}