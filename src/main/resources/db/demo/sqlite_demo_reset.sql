-- Arabic demo-data reset for SQLite.
-- This script intentionally preserves users, roles, and user_roles.
-- Run only against a demo database. It deletes all other application data.

PRAGMA foreign_keys = ON;

BEGIN TRANSACTION;

DELETE FROM cash_drawer_event;
DELETE FROM shift_session;
DELETE FROM order_items;
DELETE FROM orders;
DELETE FROM purchase_order_item;
DELETE FROM purchase_order;
DELETE FROM customer_phone;
DELETE FROM customer;
DELETE FROM products;
DELETE FROM category;
DELETE FROM vendor;
DELETE FROM settings;
DELETE FROM order_number_sequence;

-- Keep generated IDs predictable for the demo data without touching security tables.
DELETE FROM sqlite_sequence
WHERE name IN (
    'cash_drawer_event',
    'shift_session',
    'order_items',
    'orders',
    'purchase_order_item',
    'purchase_order',
    'customer_phone',
    'customer',
    'products',
    'category',
    'vendor',
    'settings',
    'order_number_sequence'
);

INSERT INTO settings (
    id, company_name, phone_number, address, tax_registration_number,
    default_theme, print_size, currency_symbol,
    is_licensed, trial_ends_at, last_accessed_at, pos_style, shift_management
) VALUES (
    1, 'متجر البقالة النموذجي', '01000000000', 'شارع التحرير، القاهرة',
    '123456789', 'SYSTEM_DEFAULT', 'A4', 'جنيه',
    1, datetime('now', '+365 days'), datetime('now'), 'HORIZONTAL', 1
);

INSERT INTO category (id, name, description, is_active) VALUES
    (1, 'الحبوب والبقوليات', 'أرز وفاصوليا وعدس وحبوب غذائية', 1),
    (2, 'المعلبات', 'أغذية معلبة جاهزة للتخزين', 1),
    (3, 'الألبان', 'حليب وجبن ومنتجات ألبان', 1),
    (4, 'المشروبات', 'مياه وعصائر ومشروبات ساخنة', 1),
    (5, 'المخبوزات', 'خبز وبسكويت ومخبوزات', 1),
    (6, 'الزيوت والصلصات', 'زيوت وخل وصلصات الطبخ', 1),
    (7, 'الحلويات', 'شوكولاتة وحلوى وسكر', 1),
    (8, 'المنظفات', 'منظفات منزلية ومنتجات غسيل', 1),
    (9, 'العناية الشخصية', 'صابون وشامبو ومنتجات عناية', 1),
    (10, 'الأدوات المنزلية', 'أدوات منزلية للاستخدام اليومي', 1);

INSERT INTO products (
    id, category_id, name, part_number, description,
    selling_price, purchase_price, stock, reorder_point, is_active, custom_fields
)
WITH RECURSIVE numbers(n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1 FROM numbers WHERE n < 100
)
SELECT
    n,
    ((n - 1) % 10) + 1,
    CASE ((n - 1) % 10) + 1
        WHEN 1 THEN CASE ((n - 1) % 5) + 1
            WHEN 1 THEN 'أرز مصري' WHEN 2 THEN 'عدس أصفر'
            WHEN 3 THEN 'فاصوليا بيضاء' WHEN 4 THEN 'برغل ناعم'
            ELSE 'حمص جاف' END
        WHEN 2 THEN CASE ((n - 1) % 5) + 1
            WHEN 1 THEN 'تونة قطع' WHEN 2 THEN 'فول مدمس'
            WHEN 3 THEN 'ذرة حلوة' WHEN 4 THEN 'صلصة طماطم معلبة'
            ELSE 'فطر معلب' END
        WHEN 3 THEN CASE ((n - 1) % 5) + 1
            WHEN 1 THEN 'حليب كامل الدسم' WHEN 2 THEN 'جبنة بيضاء'
            WHEN 3 THEN 'زبادي طبيعي' WHEN 4 THEN 'زبدة بلدية'
            ELSE 'قشطة طازجة' END
        WHEN 4 THEN CASE ((n - 1) % 5) + 1
            WHEN 1 THEN 'مياه معدنية' WHEN 2 THEN 'عصير برتقال'
            WHEN 3 THEN 'عصير مانجو' WHEN 4 THEN 'شاي أسود'
            ELSE 'قهوة مطحونة' END
        WHEN 5 THEN CASE ((n - 1) % 5) + 1
            WHEN 1 THEN 'خبز بلدي' WHEN 2 THEN 'خبز توست'
            WHEN 3 THEN 'بسكويت سادة' WHEN 4 THEN 'كرواسون'
            ELSE 'كيك فانيليا' END
        WHEN 6 THEN CASE ((n - 1) % 5) + 1
            WHEN 1 THEN 'زيت دوار الشمس' WHEN 2 THEN 'زيت زيتون'
            WHEN 3 THEN 'خل أبيض' WHEN 4 THEN 'كاتشب'
            ELSE 'مايونيز' END
        WHEN 7 THEN CASE ((n - 1) % 5) + 1
            WHEN 1 THEN 'شوكولاتة بالحليب' WHEN 2 THEN 'حلوى فواكه'
            WHEN 3 THEN 'بسكويت بالشوكولاتة' WHEN 4 THEN 'سكر أبيض'
            ELSE 'عسل طبيعي' END
        WHEN 8 THEN CASE ((n - 1) % 5) + 1
            WHEN 1 THEN 'مسحوق غسيل' WHEN 2 THEN 'سائل أطباق'
            WHEN 3 THEN 'منظف أرضيات' WHEN 4 THEN 'مطهر أسطح'
            ELSE 'مناديل ورقية' END
        WHEN 9 THEN CASE ((n - 1) % 5) + 1
            WHEN 1 THEN 'صابون يدين' WHEN 2 THEN 'شامبو شعر'
            WHEN 3 THEN 'معجون أسنان' WHEN 4 THEN 'فرشاة أسنان'
            ELSE 'كريم ترطيب' END
        ELSE CASE ((n - 1) % 5) + 1
            WHEN 1 THEN 'أكواب زجاجية' WHEN 2 THEN 'أطباق بلاستيكية'
            WHEN 3 THEN 'ملاعق معدنية' WHEN 4 THEN 'أكياس حفظ الطعام'
            ELSE 'ورق ألومنيوم' END
    END || ' ' || printf('%02d', n),
    printf('AR-%03d', n),
    'منتج تجريبي باللغة العربية للاستخدام في المتجر',
    8.00 + (n * 1.75),
    5.00 + (n * 1.10),
    20 + ((n * 7) % 81),
    5 + (n % 10),
    1,
    '{}'
FROM numbers;

INSERT INTO customer (id, name, location, shipping_company, is_active) VALUES
    (1, 'أحمد محمد', 'مدينة نصر، القاهرة', 'شركة النيل للشحن', 1),
    (2, 'سارة علي', 'المعادي، القاهرة', 'شركة النيل للشحن', 1),
    (3, 'محمد حسن', 'الهرم، الجيزة', 'شركة الأمان للشحن', 1),
    (4, 'منى إبراهيم', 'الدقي، الجيزة', 'شركة الأمان للشحن', 1),
    (5, 'عمر محمود', 'حلوان، القاهرة', 'شركة النور للشحن', 1),
    (6, 'نور خالد', 'شبرا، القاهرة', 'شركة النور للشحن', 1),
    (7, 'يوسف عادل', 'المنصورة، الدقهلية', 'شركة الدلتا للشحن', 1),
    (8, 'مريم سامي', 'طنطا، الغربية', 'شركة الدلتا للشحن', 1),
    (9, 'خالد مصطفى', 'الزقازيق، الشرقية', 'شركة الشرقية للشحن', 1),
    (10, 'ريم طارق', 'بنها، القليوبية', 'شركة الشرقية للشحن', 1),
    (11, 'حسن عبد الله', 'أسيوط، أسيوط', 'شركة الصعيد للشحن', 1),
    (12, 'هدى يوسف', 'سوهاج، سوهاج', 'شركة الصعيد للشحن', 1),
    (13, 'كريم أشرف', 'الإسكندرية، الإسكندرية', 'شركة البحر للشحن', 1),
    (14, 'دينا وائل', 'سيدي جابر، الإسكندرية', 'شركة البحر للشحن', 1),
    (15, 'طارق جابر', 'بورسعيد، بورسعيد', 'شركة القناة للشحن', 1),
    (16, 'إيمان رجب', 'الإسماعيلية، الإسماعيلية', 'شركة القناة للشحن', 1),
    (17, 'علي حمدي', 'الفيوم، الفيوم', 'شركة الصعيد للشحن', 1),
    (18, 'بسمة فتحي', 'المنيا، المنيا', 'شركة الصعيد للشحن', 1),
    (19, 'وليد سمير', 'دمياط، دمياط', 'شركة الساحل للشحن', 1),
    (20, 'جنى فؤاد', 'كفر الشيخ، كفر الشيخ', 'شركة الساحل للشحن', 1);

INSERT INTO customer_phone (customer_id, type, is_primary, phone_number)
SELECT id, 'MOBILE_PHONE', 1, printf('010%08d', id)
FROM customer;

INSERT INTO vendor (id, name, location, landline, mobile, is_active) VALUES
    (1, 'شركة الخير للتوريدات', 'القاهرة', '0223456001', '01110000001', 1),
    (2, 'مؤسسة الأمل التجارية', 'الجيزة', '0234567002', '01110000002', 1),
    (3, 'شركة النيل للمواد الغذائية', 'القليوبية', '0245678003', '01110000003', 1),
    (4, 'مخازن البركة', 'الإسكندرية', '0356789004', '01110000004', 1),
    (5, 'شركة الرواد للتجارة', 'الدقهلية', '0501234005', '01110000005', 1),
    (6, 'مؤسسة زهرة المدينة', 'الغربية', '0402345006', '01110000006', 1),
    (7, 'شركة الصفوة للتوزيع', 'الشرقية', '0553456007', '01110000007', 1),
    (8, 'مخازن مصر الحديثة', 'المنوفية', '0484567008', '01110000008', 1),
    (9, 'شركة البيت الآمن', 'أسيوط', '0885678009', '01110000009', 1),
    (10, 'تجارة المستقبل', 'سوهاج', '0936789010', '01110000010', 1),
    (11, 'شركة الوادي الأخضر', 'الفيوم', '0847890011', '01110000011', 1),
    (12, 'مؤسسة القمة', 'المنيا', '0868901012', '01110000012', 1),
    (13, 'شركة الميناء للتوريد', 'بورسعيد', '0669012013', '01110000013', 1),
    (14, 'مخازن الدلتا', 'كفر الشيخ', '0470123014', '01110000014', 1),
    (15, 'شركة الأصيل', 'دمياط', '0571234015', '01110000015', 1),
    (16, 'مؤسسة الوفاء', 'الإسماعيلية', '0642345016', '01110000016', 1),
    (17, 'شركة سوق الجملة', 'البحيرة', '0453456017', '01110000017', 1),
    (18, 'مخازن السلام', 'قنا', '0964567018', '01110000018', 1),
    (19, 'شركة المذاق الطيب', 'الأقصر', '0955678019', '01110000019', 1),
    (20, 'مؤسسة الإمداد', 'أسوان', '0976789020', '01110000020', 1);

-- Create ten sample sales using the first preserved user.
INSERT INTO orders (
    id, user_id, user_name, total, revenue, discount, order_number, created_at
)
SELECT
    n,
    u.id,
    u.name,
    ROUND((p.selling_price * 2) - n, 2),
    ROUND(((p.selling_price - p.purchase_price) * 2) - n, 2),
    n,
    printf('%d', 1000 + n),
    datetime('now', printf('-%d days', n))
FROM (SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
      UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8
      UNION ALL SELECT 9 UNION ALL SELECT 10) sample
JOIN products p ON p.id = n
JOIN (SELECT id, name FROM users ORDER BY id LIMIT 1) u;

INSERT INTO order_items (
    order_id, product_id, product_name, product_selling_price,
    product_purchase_price, quantity, sub_discount
)
SELECT
    o.id, p.id, p.name, p.selling_price, p.purchase_price, 2, o.discount
FROM orders o
JOIN products p ON p.id = o.id;

-- Create ten sample purchase orders using the first preserved user.
INSERT INTO purchase_order (
    id, user_id, vendor_id, user_name, total, discount, order_number, created_at
)
SELECT
    n,
    u.id,
    n,
    u.name,
    ROUND((p.purchase_price * 10) - n, 2),
    n,
    printf('%d', 2000 + n),
    datetime('now', printf('-%d days', n + 2))
FROM (SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
      UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8
      UNION ALL SELECT 9 UNION ALL SELECT 10) sample
JOIN products p ON p.id = n
JOIN (SELECT id, name FROM users ORDER BY id LIMIT 1) u;

INSERT INTO purchase_order_item (
    order_id, product_id, product_name, product_purchase_price, quantity, sub_discount
)
SELECT po.id, p.id, p.name, p.purchase_price, 10, po.discount
FROM purchase_order po
JOIN products p ON p.id = po.id;

INSERT INTO order_number_sequence (id) VALUES (10);

INSERT INTO shift_session (
    id, user_id, status, start_time, end_time, starting_float, expected_cash, counted_cash
)
SELECT
    1, id, 'مغلقة', datetime('now', '-1 day'), datetime('now', '-1 day', '+8 hours'),
    1000.00, 2500.00, 2480.00
FROM (SELECT id FROM users ORDER BY id LIMIT 1);

INSERT INTO cash_drawer_event (shift_id, event_type, amount, reason, created_at) VALUES
    (1, 'إيداع', 1000.00, 'رأس مال بداية الوردية', datetime('now', '-1 day')),
    (1, 'سحب', 20.00, 'مصروفات تشغيلية', datetime('now', '-1 day', '+4 hours'));

COMMIT;

PRAGMA foreign_keys = ON;
