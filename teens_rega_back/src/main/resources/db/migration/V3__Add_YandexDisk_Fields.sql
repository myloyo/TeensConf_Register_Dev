ALTER TABLE payment_receipts ADD yandex_disk_url VARCHAR(500);
ALTER TABLE payment_receipts ADD yandex_disk_uploaded BOOLEAN DEFAULT FALSE;