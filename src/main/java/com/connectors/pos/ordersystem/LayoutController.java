package com.connectors.pos.ordersystem;

import com.connectors.pos.settings.PosStyle;
import com.connectors.pos.settings.Settings;
import com.connectors.pos.settings.SettingsGlobalInjector;
import com.connectors.pos.settings.settingsdtos.SettingsResponseDto;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
@RequiredArgsConstructor
@Controller
@RequestMapping("/layout")
public class LayoutController {
private String layoutFile;
    private final SettingsGlobalInjector settings;

    @ModelAttribute
private void setPosStyle(){
   SettingsResponseDto sets = settings.getSettings();

    PosStyle style=sets.posStyle();
    if(style.equals(PosStyle.HORIZONTAL)){
        layoutFile="fragments/layout";
    }
    else{

        layoutFile="fragments/layout-custom";
    }
}


@GetMapping
    public String getMainPage(){

        return layoutFile;
    }
}
