package br.com.sawcunhaos.foundation.utils.listener;

import br.com.sawcunhaos.foundation.utils.specification.ScosStartupListener;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
@Order(1)
@Log4j2
public class ScosOnStartupListener implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired(required = false)
    private List<ScosStartupListener> scosStartupListener;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("InsideSoftwaresOnStartupListener#onApplicationEvent() - Init");
        if(Objects.nonNull(scosStartupListener)) scosStartupListener.forEach(scosStartupListener1 -> scosStartupListener1.onStartupSystem(event));
        log.info("InsideSoftwaresOnStartupListener#onApplicationEvent() - Final");
    }

}
