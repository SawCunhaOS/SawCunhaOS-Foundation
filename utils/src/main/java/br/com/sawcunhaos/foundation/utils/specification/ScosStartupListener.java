package br.com.sawcunhaos.foundation.utils.specification;

import org.springframework.boot.context.event.ApplicationReadyEvent;

public interface ScosStartupListener {
    void onStartupSystem(ApplicationReadyEvent event);
}
