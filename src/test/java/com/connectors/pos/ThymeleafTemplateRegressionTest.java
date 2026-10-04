package com.connectors.pos;

import com.connectors.pos.charts.Statistics;
import com.connectors.pos.charts.TopSellingItemDto;
import com.connectors.pos.charts.WorstSellingItemDto;
import com.connectors.pos.customersystem.customerdtos.CustomerCreateDto;
import com.connectors.pos.customersystem.customerdtos.CustomerUpdateDto;
import com.connectors.pos.customersystem.customerdtos.CustomerViewDto;
import com.connectors.pos.ordersystem.orderdtos.CartItemView;
import com.connectors.pos.ordersystem.orderdtos.CustomerSummary;
import com.connectors.pos.ordersystem.orderdtos.OrderItemResponseDto;
import com.connectors.pos.ordersystem.orderdtos.OrderResponseDto;
import com.connectors.pos.products.categorydtos.CategoryCreateDto;
import com.connectors.pos.products.categorydtos.CategoryResponseDto;
import com.connectors.pos.products.productdtos.CreateProductDto;
import com.connectors.pos.products.productdtos.ProductResponseDto;
import com.connectors.pos.products.productdtos.ProductUpdateDto;
import com.connectors.pos.purchasesystem.Vendor;
import com.connectors.pos.purchasesystem.purchasedtos.PurchaseOrderResponseDto;
import com.connectors.pos.purchasesystem.purchasedtos.PurchaseReturnLineOption;
import com.connectors.pos.purchasesystem.purchasedtos.PurchaseReturnRequest;
import com.connectors.pos.purchasesystem.purchasedtos.VendorCreateDto;
import com.connectors.pos.settings.PosStyle;
import com.connectors.pos.settings.PrintSize;
import com.connectors.pos.settings.Theme;
import com.connectors.pos.settings.settingsdtos.SettingsResponseDto;
import com.connectors.pos.settings.settingsdtos.SettingsUpdateDto;
import com.connectors.pos.users.userdtos.CreateUserDto;
import com.connectors.pos.users.userdtos.UserLoginDto;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.support.RequestContext;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.context.webmvc.SpringWebMvcThymeleafRequestContext;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThymeleafTemplateRegressionTest {
    private static final SpringTemplateEngine TEMPLATE_ENGINE = new SpringTemplateEngine();
    private static final MockServletContext SERVLET_CONTEXT = new MockServletContext();
    private static final JakartaServletWebApplication WEB_APPLICATION =
            JakartaServletWebApplication.buildApplication(SERVLET_CONTEXT);

    @BeforeAll
    static void configureTemplateEngine() {
        StaticWebApplicationContext applicationContext = new StaticWebApplicationContext();
        applicationContext.setServletContext(SERVLET_CONTEXT);
        applicationContext.refresh();
        SERVLET_CONTEXT.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, applicationContext);

        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCacheable(false);
        TEMPLATE_ENGINE.setTemplateResolver(resolver);
    }

    @Test
    void everyTemplateParsesAndRendersWithControllerModelFixtures() throws IOException {
        Map<String, Object> model = controllerModel();
        Path templatesRoot = Path.of("src/main/resources/templates");
        Set<String> safelyRenderedTemplates;
        try (var paths = Files.walk(templatesRoot)) {
            safelyRenderedTemplates = paths
                    .filter(path -> path.toString().endsWith(".html"))
                    .map(path -> templatesRoot.relativize(path).toString()
                            .replace('\\', '/').replaceAll("\\.html$", ""))
                    .collect(Collectors.toSet());
        }

        assertEquals(43, safelyRenderedTemplates.size(), "The regression inventory must cover all templates.");
        assertTrue(safelyRenderedTemplates.containsAll(expectedTemplates()));

        for (String template : safelyRenderedTemplates) {
            Map<String, Object> templateModel = new HashMap<>(model);
            if (template.equals("sale-returns")) {
                templateModel.put("sales", model.get("orders"));
            } else if (template.equals("fragments/sale-return-form")) {
                templateModel.put("sale", model.get("orderResult"));
                templateModel.put("returnItems", List.of());
                templateModel.put("returnHistory", List.of());
                templateModel.put("returnRequest", new com.connectors.pos.ordersystem.orderdtos.SaleReturnRequest(
                        null, null, false, ""));
            }
            String output = render(template, templateModel);
            assertNotNull(output, template + " should produce rendered output");
            assertFalse(output.isBlank(), template + " should not render as blank output");
        }
    }

    @Test
    void salesCartFragmentRendersCartBindingsAndCheckoutActions() {
        HtmlProbe html = HtmlProbe.parse(render("fragments/cart :: cart", controllerModel()));

        html.require("form", "id", "active-cart-form");
        html.require("input", "name", "itemsList[0].quantity");
        html.require("input", "name", "itemsList[0].customSellingPrice");
        html.require("input", "id", "remaining");
        html.require("input", "name", "discount");
        html.require("input", "name", "paid");
        html.require("span", "id", "taxable-subtotal");
        html.require("span", "id", "tax-amount");
        html.require("select", "name", "paymentMethod");
        html.require("input", "name", "paymentReference");
        html.require("strong", "id", "cash-change");
        assertNotNull(html.find("option", "value", "BANK_TRANSFER"));
        assertNotNull(html.find("option", "value", "E_WALLET"));

        ParsedTag checkout = html.require("button", "hx-post", "/pos/check-out");
        List<String> includedFields = List.of(checkout.attribute("hx-include").split("\\s*,\\s*"));
        assertTrue(includedFields.containsAll(List.of(
                "#active-cart-form", "#disc", "#paid", "#customerInfo", "#remaining",
                "#paymentMethod", "#paymentReference")));
        assertEquals("#cart-zone", checkout.attribute("hx-target"));
        assertNotNull(html.find("button", "hx-delete", "/pos/clear"),
                "The cart clear action must remain an HTMX delete request.");
    }

    @Test
    void productFormsRenderTheirBoundFieldsAndHtmxTargets() {
        Map<String, Object> model = controllerModel();
        HtmlProbe editor = HtmlProbe.parse(render("fragments/update-fragment :: update-popup", model));
        ParsedTag updateForm = editor.require("form", "id", "form-update");
        assertEquals("/products/42", updateForm.attribute("hx-patch"));
        assertEquals("#tbody", updateForm.attribute("hx-target"));
        editor.require("input", "name", "name");
        editor.require("input", "name", "partNumber");
        editor.require("input", "name", "sellingPrice");
        editor.require("input", "name", "customFields[Color]");

        HtmlProbe create = HtmlProbe.parse(render("fragments/auth-messages :: pop-up", model));
        ParsedTag createForm = create.require("form", "id", "form1");
        assertEquals("/products", createForm.attribute("hx-post"));
        assertEquals("#tbody", createForm.attribute("hx-target"));
        create.require("input", "name", "name");
        create.require("select", "name", "categoryId");
        assertNotNull(create.find("button", "hx-get", "/products/custom-field-row"));
    }

    @Test
    void customerAndAuthenticationFormsRenderRealFormBindings() {
        Map<String, Object> model = controllerModel();
        HtmlProbe customerForm = HtmlProbe.parse(render("fragments/customer-form :: customer-pop-up", model));
        ParsedTag form = customerForm.require("form", "id", "new-cust-form");
        assertEquals("/customers", form.attribute("hx-post"));
        assertEquals("#customer-fragment", form.attribute("hx-target"));
        customerForm.require("input", "name", "name");
        customerForm.require("input", "name", "location");
        customerForm.require("input", "name", "shippingCompany");

        HtmlProbe login = HtmlProbe.parse(render("login", model));
        login.require("input", "name", "email");
        login.require("input", "name", "password");
        HtmlProbe register = HtmlProbe.parse(render("register", model));
        register.require("input", "name", "name");
        register.require("input", "name", "email");
        register.require("input", "name", "password");

        HtmlProbe settings = HtmlProbe.parse(render("settings :: settings-fragment", model));
        settings.require("input", "name", "taxRate");

        HtmlProbe invoice = HtmlProbe.parse(render("invoice-print-A4", model));
        invoice.require("td", "id", "invoice-payment-method");
        invoice.require("td", "id", "invoice-cash-received");
    }

    @Test
    void vendorReturnListAndFormRenderTheirHtmxActionsAndBoundFields() {
        Map<String, Object> model = controllerModel();
        HtmlProbe list = HtmlProbe.parse(render("purchase-returns :: purchase-returns", model));
        assertNotNull(list.find("button", "hx-get", "/purchase/returns/31"));
        list.require("table", "role", "grid");

        HtmlProbe form = HtmlProbe.parse(render("fragments/purchase-return-form :: return-form", model));
        ParsedTag submit = form.require("form", "hx-post", "/purchase/returns/31");
        assertEquals("#main-window", submit.attribute("hx-target"));
        form.require("select", "name", "purchaseItemId");
        form.require("input", "name", "quantity");
        form.require("input", "name", "reason");
        assertNotNull(form.find("button", "type", "submit"));
    }

    @Test
    void saleReturnListAndFormRenderAdminReturnAndVoidActions() {
        Map<String, Object> model = controllerModel();
        HtmlProbe list = HtmlProbe.parse(render("sale-returns :: sale-returns", Map.of(
                "sales", model.get("orders"))));
        assertNotNull(list.find("button", "hx-get", "/pos/returns/25"));
        list.require("table", "role", "grid");

        Map<String, Object> formModel = new HashMap<>(model);
        formModel.put("sale", model.get("orderResult"));
        formModel.put("returnRequest", new com.connectors.pos.ordersystem.orderdtos.SaleReturnRequest(
                null, null, false, ""));
        formModel.put("returnItems", List.of());
        formModel.put("returnHistory", List.of());
        HtmlProbe form = HtmlProbe.parse(render("fragments/sale-return-form :: sale-return-form", formModel));
        form.require("form", "hx-post", "/pos/returns/25");
        form.require("form", "hx-post", "/pos/returns/25/void");
        form.require("select", "name", "orderItemId");
        form.require("input", "name", "reason");
        form.require("table", "role", "grid");
    }

    private static String render(String template, Map<String, Object> model) {
        MockHttpServletRequest request = new MockHttpServletRequest(SERVLET_CONTEXT);
        request.setPreferredLocales(List.of(Locale.ENGLISH));
        request.addParameter("keyword", "brake");
        request.addParameter("name", "Ada");
        request.addParameter("orderNum", "ORD-100");
        request.addParameter("custName", "Ada");
        request.addParameter("dateRange", "2026-01-01 to 2026-01-31");
        model.forEach((name, value) -> {
            if (Set.of("createDto", "categoryDto", "customerCreate", "customer", "supplierCreate",
                    "supplierUpdate", "settings", "userForm", "loginForm", "updateProduct").contains(name)) {
                request.setAttribute(BindingResult.MODEL_KEY_PREFIX + name,
                        new BeanPropertyBindingResult(value, name));
            }
        });
        MockHttpServletResponse response = new MockHttpServletResponse();
        WebContext context = new WebContext(WEB_APPLICATION.buildExchange(request, response), Locale.ENGLISH, model);
        RequestContext requestContext = new RequestContext(request, response, SERVLET_CONTEXT, model);
        context.setVariable("springRequestContext", requestContext);
        context.setVariable("springMacroRequestContext", requestContext);
        context.setVariable("thymeleafRequestContext",
                new SpringWebMvcThymeleafRequestContext(requestContext, request));
        String[] fragmentView = template.split("\\s+::\\s+", 2);
        return fragmentView.length == 2
                ? TEMPLATE_ENGINE.process(fragmentView[0], Set.of(fragmentView[1]), context)
                : TEMPLATE_ENGINE.process(template, context);
    }

    private static Map<String, Object> controllerModel() {
        BigDecimal amount = new BigDecimal("12.50");
        LocalDateTime now = LocalDateTime.of(2026, 1, 15, 10, 30);
        ProductResponseDto product = new ProductResponseDto(
                42L, "Brake pad", "BP-42", "Front brake pad", amount, new BigDecimal("7.00"),
                8L, now, now, 3L, "123456789012", 2L, Map.of("Color", "Black"));
        CustomerViewDto customerView = new CustomerViewDto(7L, "Ada Customer", "Cairo", "FastShip", "01000000000");
        Vendor vendor = new Vendor(9L, "Parts Vendor", "Giza", "023000000", "01011111111", true);
        OrderItemResponseDto orderItem = new OrderItemResponseDto(
                15L, product.name(), product.sellingPrice(), product.purchasePrice(), 2,
                new BigDecimal("25.00"), BigDecimal.ZERO, product.barcode(), product.id());
        OrderResponseDto order = new OrderResponseDto(
                25L, "cashier", new BigDecimal("25.00"), BigDecimal.ZERO, amount,
                new BigDecimal("12.50"), now, 1L, customerView.id(), customerView.name(),
                List.of(orderItem), "ORD-100");
        Page<CustomerViewDto> customerPage = page(customerView);
        Page<ProductResponseDto> productPage = page(product);
        Page<Vendor> vendorPage = page(vendor);
        Page<OrderResponseDto> orderPage = page(order);
        Map<String, Object> model = new HashMap<>();

        model.put("globalSettings", new SettingsResponseDto("Garage POS", "01012345678", "Cairo",
                "TAX-42", Theme.LIGHT, PrintSize.A4, "EGP", PosStyle.HORIZONTAL, null, true));
        model.put("settings", new SettingsUpdateDto("Garage POS", "01012345678", "Cairo", "TAX-42",
                Theme.LIGHT, PrintSize.A4, "EGP", PosStyle.HORIZONTAL, null, true));
        model.put("userForm", new CreateUserDto("Ada", "ada@example.test", "Password1!"));
        model.put("loginForm", new UserLoginDto("ada@example.test", "Password1!"));
        model.put("demoCredentialsEnabled", false);
        model.put("_csrf", new DefaultCsrfToken("X-XSRF-TOKEN", "_csrf", "test-token"));
        model.put("customerCreate", new CustomerCreateDto("Ada Customer", "Cairo", "FastShip", List.of()));
        model.put("customer", new CustomerUpdateDto("Ada Customer", "Cairo", "FastShip", "01000000000"));
        model.put("customerId", customerView.id());
        model.put("custs", customerPage);
        model.put("customers", List.of(customerView));
        model.put("supplierCreate", new VendorCreateDto(null, "Parts Vendor", "Giza", "01011111111", "023000000"));
        model.put("supplierUpdate", new VendorCreateDto(vendor.getId(), vendor.getName(), vendor.getLocation(),
                vendor.getMobile(), vendor.getLandline()));
        model.put("vendors", vendorPage);
        model.put("suppliers", List.of(vendor));
        model.put("currentSupplierId", vendor.getId());
        PurchaseOrderResponseDto purchaseOrder = new PurchaseOrderResponseDto(
                31L, "Ada", new BigDecimal("25.00"), BigDecimal.ZERO, new BigDecimal("5.00"),
                new BigDecimal("20.00"), BigDecimal.ZERO, now, 1L, vendor.getId(), vendor.getName(),
                List.of(), "PUR-31");
        model.put("purchaseOrders", page(purchaseOrder));
        model.put("purchaseOrderId", purchaseOrder.id());
        model.put("returnRequest", new PurchaseReturnRequest(null, null, ""));
        model.put("returnItems", List.of(new PurchaseReturnLineOption(
                41L, product.name(), 5, 1, 4, new BigDecimal("7.00"))));
        model.put("allProducts", productPage);
        model.put("products", productPage);
        model.put("results", productPage);
        model.put("keyword", "brake");
        model.put("product", product);
        model.put("catMap", Map.of(3L, "Brakes"));
        model.put("categories", List.of(new CategoryResponseDto(3L, "Brakes", "Brake parts")));
        model.put("categoryDto", new CategoryCreateDto("Brakes", "Brake parts"));
        model.put("createDto", new CreateProductDto(product.name(), product.partNumber(), product.description(),
                product.sellingPrice(), product.purchasePrice(), product.stock(), product.categoryId(),
                product.barcode(), product.reorderPoint(), product.customFields()));
        model.put("updateProduct", new ProductUpdateDto(product.name(), product.partNumber(), product.description(),
                product.sellingPrice(), product.purchasePrice(), product.stock(), product.categoryId(),
                product.barcode(), product.reorderPoint(), product.customFields()));
        model.put("prodId", product.id());
        model.put("catName", "Brakes");
        model.put("catId", product.categoryId());
        model.put("cartItems", List.of(new CartItemView(product.id(), product.name(), 2, BigDecimal.ZERO,
                product.sellingPrice(), new BigDecimal("25.00"), product.barcode(), product.name(),
                product.sellingPrice(), product.purchasePrice())));
        model.put("globalDiscount", BigDecimal.ZERO);
        model.put("discount", BigDecimal.ZERO);
        model.put("grandTotal", new BigDecimal("25.00"));
        model.put("paid", amount);
        model.put("remaining", new BigDecimal("12.50"));
        model.put("ordId", null);
        model.put("orderNumber", "ORD-100");
        model.put("currentCustomerId", customerView.id());
        model.put("order", order);
        model.put("orderResults", List.of(order));
        model.put("orderResult", order);
        model.put("customerName", customerView.name());
        model.put("mode", "sales");
        model.put("allow_shift", true);
        model.put("activeShift", Map.of("id", 4L));
        model.put("closedShift", Map.of("expectedCash", amount, "countedCash", new BigDecimal("13.00")));
        model.put("count", 1L);
        model.put("summary", new CustomerSummary(customerView.name(), now.minusDays(14), now,
                new BigDecimal("50.00"), amount, new BigDecimal("37.50"), orderPage));
        model.put("orders", orderPage);
        model.put("startDt", now.minusDays(14));
        model.put("endDt", now);
        model.put("total", new BigDecimal("50.00"));
        model.put("res", new Statistics(new BigDecimal("50.00"), new BigDecimal("18.00"), now));
        model.put("top", List.of(new TopSellingItemDto(product.name(), 4L)));
        model.put("least", List.of(new WorstSellingItemDto("Air filter", 1L)));
        model.put("errorMessage", "Invalid request");
        return model;
    }

    private static <T> Page<T> page(T value) {
        return new PageImpl<>(List.of(value), PageRequest.of(0, 5), 8);
    }

    private static Set<String> expectedTemplates() {
        return Set.of(
                "Customer-update", "barcode-label", "customer-form2", "customers", "customers2",
                "invoice-print-A4", "invoice-print-A5", "invoice-print-thermal", "login", "pos",
                "products-Reorder-Point", "products", "purchase-cart", "purchase-returns",
                "register", "sale-returns", "settings",
                "shift", "summary", "vendor-create-vendorpage", "vendor-update-popup",
                "vendors", "fragments/auth-messages", "fragments/cart", "fragments/custom-field-row",
                "fragments/customer-form", "fragments/diff-pos-layout", "fragments/layout-custom",
                "fragments/layout", "fragments/order-search-results", "fragments/order-sum-res-fragment",
                "fragments/pos-custom", "fragments/purchase-return-form", "fragments/sale-return-form",
                "fragments/search-results",
                "fragments/stat", "fragments/statics",
                "fragments/supplier-form", "fragments/update-fragment");
    }

    private record ParsedTag(String name, Map<String, String> attributes) {
        String attribute(String name) {
            return attributes.get(name);
        }
    }

    private static final class HtmlProbe {
        private final List<ParsedTag> tags;

        private HtmlProbe(List<ParsedTag> tags) {
            this.tags = tags;
        }

        static HtmlProbe parse(String html) {
            List<ParsedTag> tags = new ArrayList<>();
            try {
                new ParserDelegator().parse(new StringReader(html), new HTMLEditorKit.ParserCallback() {
                    @Override
                    public void handleStartTag(HTML.Tag tag, MutableAttributeSet attributes, int position) {
                        tags.add(new ParsedTag(tag.toString().toLowerCase(Locale.ROOT), attributes(attributes)));
                    }

                    @Override
                    public void handleSimpleTag(HTML.Tag tag, MutableAttributeSet attributes, int position) {
                        tags.add(new ParsedTag(tag.toString().toLowerCase(Locale.ROOT), attributes(attributes)));
                    }

                    private Map<String, String> attributes(MutableAttributeSet attrs) {
                        Map<String, String> result = new HashMap<>();
                        Enumeration<?> names = attrs.getAttributeNames();
                        while (names.hasMoreElements()) {
                            Object key = names.nextElement();
                            Object value = attrs.getAttribute(key);
                            result.put(key.toString().toLowerCase(Locale.ROOT), value == null ? "" : value.toString());
                        }
                        return result;
                    }
                }, true);
            } catch (IOException exception) {
                throw new AssertionError("Rendered HTML should be parseable", exception);
            }
            return new HtmlProbe(tags);
        }

        ParsedTag require(String tag, String attribute, String value) {
            ParsedTag found = find(tag, attribute, value);
            assertNotNull(found, () -> "Expected <" + tag + "> with " + attribute + "=\"" + value + "\"");
            return found;
        }

        ParsedTag find(String tag, String attribute, String value) {
            return tags.stream()
                    .filter(candidate -> candidate.name().equals(tag))
                    .filter(candidate -> value.equals(candidate.attributes().get(attribute)))
                    .findFirst()
                    .orElse(null);
        }
    }
}
