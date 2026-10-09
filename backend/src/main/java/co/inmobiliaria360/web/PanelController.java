package co.inmobiliaria360.web;

import co.inmobiliaria360.service.CarteraService;
import co.inmobiliaria360.service.PanelService;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api")
public class PanelController {

    private final PanelService panel;
    private final CarteraService cartera;

    public PanelController(PanelService panel, CarteraService cartera) {
        this.panel = panel;
        this.cartera = cartera;
    }

    @GetMapping("/panel")
    public PanelService.Panel panel(@RequestParam(defaultValue = "")
                                    @Pattern(regexp = "^(\\d{4}-(0[1-9]|1[0-2]))?$", message = "debe tener formato AAAA-MM") String periodo) {
        return panel.panel(periodo);
    }

    @GetMapping("/cartera")
    public CarteraService.Cartera cartera() {
        return cartera.calcular();
    }
}
