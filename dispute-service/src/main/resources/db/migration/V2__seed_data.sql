-- DisputeDesk :: synthetic seed data (Part 0)
-- ALL DATA IS FICTIONAL. Merchants, cardholders and reason codes are invented.
--
-- Dates are RELATIVE to now() so filing windows stay valid whenever a reader
-- runs this — a hard requirement for the eval suite in Part 8.
--
-- Every row below exists to exercise a specific scenario. See docs/scenarios.md.

INSERT INTO cardholders (id, full_name, email, segment, preferred_language, customer_since) VALUES
 ('CH-1001', 'Priya Mehta',    'priya.mehta@example.com',    'STANDARD', 'en', DATE '2019-03-14'),
 ('CH-1002', 'Arjun Rao',      'arjun.rao@example.com',      'PREMIUM',  'en', DATE '2015-08-02'),
 ('CH-1003', 'Meera Iyer',     'meera.iyer@example.com',     'STANDARD', 'hi', DATE '2022-11-20'),
 ('CH-1004', 'Karan Malhotra', 'karan.malhotra@example.com', 'STANDARD', 'en', DATE '2021-06-09');

INSERT INTO cards (id, cardholder_id, last4, product, status) VALUES
 ('CRD-1001', 'CH-1001', '4417', 'Meridian Rewards Credit',  'ACTIVE'),
 ('CRD-1002', 'CH-1002', '8823', 'Meridian Signature Credit','ACTIVE'),
 ('CRD-1003', 'CH-1003', '1290', 'Meridian Everyday Debit',  'ACTIVE'),
 ('CRD-1004', 'CH-1004', '5561', 'Meridian Rewards Credit',  'ACTIVE');

INSERT INTO merchants (id, name, statement_descriptor, mcc, category, country) VALUES
 ('M-URBANNEST',   'UrbanNest Home Decor',   'URBANNEST*ONLINE',    '5712', 'Home furnishings',        'IN'),
 ('M-STREAMFLIX',  'StreamFlix Media',       'STREAMFLIX*SUBSCR',   '4899', 'Streaming subscription',  'IN'),
 ('M-QUICKCART',   'QuickCart Marketplace',  'QCART MKTPLACE BLR',  '5399', 'Online marketplace',      'IN'),
 ('M-TAPRI',       'Tapri Tea House',        'SQ *TPR HSPTLTY',     '5814', 'Cafe',                    'IN'),
 ('M-NEONBYTE',    'NeonByte Games Ltd',     'NBG*DIGITAL LTD',     '5816', 'Digital games',           'GB'),
 ('M-SKYHOP',      'SkyHop Airways',         'SKYHOP AIR 0981',     '4511', 'Airline',                 'IN'),
 ('M-FRESHBASKET', 'FreshBasket Grocery',    'FRESHBASKET GROCERY', '5411', 'Grocery',                 'IN'),
 ('M-FITZONE',     'FitZone Gyms',           'FITZONE*MEMBERSHIP',  '7997', 'Gym membership',          'IN');

-- ---------------------------------------------------------------- Priya (CH-1001)
INSERT INTO transactions (id, card_id, merchant_id, type, status, amount, currency, recurring, original_transaction_id, authorized_at, posted_at) VALUES
 ('TXN-100101','CRD-1001','M-FRESHBASKET','PURCHASE','POSTED', 1842.50,'INR',false,null, now()-interval '12 days', now()-interval '11 days'),
 -- S1 duplicate charge: same merchant, same amount, 3 minutes apart
 ('TXN-100102','CRD-1001','M-URBANNEST',  'PURCHASE','POSTED', 4999.00,'INR',false,null, now()-interval '6 days', now()-interval '5 days'),
 ('TXN-100103','CRD-1001','M-URBANNEST',  'PURCHASE','POSTED', 4999.00,'INR',false,null, now()-interval '6 days'+interval '3 minutes', now()-interval '5 days'),
 -- S2 cancelled recurring: she cancelled ~40 days ago, the last charge should not exist
 ('TXN-100104','CRD-1001','M-STREAMFLIX', 'PURCHASE','POSTED',  649.00,'INR',true, null, now()-interval '62 days', now()-interval '61 days'),
 ('TXN-100105','CRD-1001','M-STREAMFLIX', 'PURCHASE','POSTED',  649.00,'INR',true, null, now()-interval '32 days', now()-interval '31 days'),
 ('TXN-100106','CRD-1001','M-STREAMFLIX', 'PURCHASE','POSTED',  649.00,'INR',true, null, now()-interval '2 days',  now()-interval '1 day'),
 -- S3 outside filing window (150 days > 120)
 ('TXN-100107','CRD-1001','M-SKYHOP',     'PURCHASE','POSTED',12480.00,'INR',false,null, now()-interval '151 days', now()-interval '150 days');

-- ---------------------------------------------------------------- Arjun (CH-1002)
INSERT INTO transactions (id, card_id, merchant_id, type, status, amount, currency, recurring, original_transaction_id, authorized_at, posted_at) VALUES
 -- S4 goods not received (expected delivery ~15 days ago)
 ('TXN-100201','CRD-1002','M-QUICKCART',  'PURCHASE','POSTED', 8750.00,'INR',false,null, now()-interval '26 days', now()-interval '25 days'),
 -- S5 already refunded in full by the merchant: nothing left to dispute
 ('TXN-100202','CRD-1002','M-QUICKCART',  'PURCHASE','POSTED', 2300.00,'INR',false,null, now()-interval '41 days', now()-interval '40 days'),
 ('TXN-100203','CRD-1002','M-QUICKCART',  'REFUND',  'POSTED', 2300.00,'INR',false,'TXN-100202', now()-interval '34 days', now()-interval '33 days'),
 ('TXN-100204','CRD-1002','M-FRESHBASKET','PURCHASE','POSTED', 3120.00,'INR',false,null, now()-interval '4 days', now()-interval '3 days'),
 -- S6 still pending: cannot be disputed yet
 ('TXN-100205','CRD-1002','M-URBANNEST',  'PURCHASE','PENDING',1499.00,'INR',false,null, now()-interval '1 day', null);

-- ---------------------------------------------------------------- Meera (CH-1003)
INSERT INTO transactions (id, card_id, merchant_id, type, status, amount, currency, recurring, original_transaction_id, authorized_at, posted_at) VALUES
 -- S7 "I don't recognise this" but it is legitimate — the descriptor is just cryptic
 ('TXN-100301','CRD-1003','M-TAPRI',      'PURCHASE','POSTED',  180.00,'INR',false,null, now()-interval '5 days', now()-interval '4 days'),
 -- S8 genuinely suspicious: foreign digital-goods merchant, twice in 10 minutes
 ('TXN-100302','CRD-1003','M-NEONBYTE',   'PURCHASE','POSTED',   59.99,'USD',false,null, now()-interval '3 days', now()-interval '2 days'),
 ('TXN-100303','CRD-1003','M-NEONBYTE',   'PURCHASE','POSTED',   59.99,'USD',false,null, now()-interval '3 days'+interval '10 minutes', now()-interval '2 days'),
 ('TXN-100304','CRD-1003','M-FRESHBASKET','PURCHASE','POSTED',  960.00,'INR',false,null, now()-interval '8 days', now()-interval '7 days');

-- ---------------------------------------------------------------- Karan (CH-1004)
INSERT INTO transactions (id, card_id, merchant_id, type, status, amount, currency, recurring, original_transaction_id, authorized_at, posted_at) VALUES
 ('TXN-100401','CRD-1004','M-FITZONE',    'PURCHASE','POSTED', 2500.00,'INR',true, null, now()-interval '96 days', now()-interval '95 days'),
 ('TXN-100402','CRD-1004','M-FITZONE',    'PURCHASE','POSTED', 2500.00,'INR',true, null, now()-interval '66 days', now()-interval '65 days'),
 ('TXN-100403','CRD-1004','M-FITZONE',    'PURCHASE','POSTED', 2500.00,'INR',true, null, now()-interval '36 days', now()-interval '35 days'),
 -- S9 new dispute from a cardholder with a history of lost disputes (memory/profile, Part 6)
 ('TXN-100404','CRD-1004','M-FITZONE',    'PURCHASE','POSTED', 2500.00,'INR',true, null, now()-interval '6 days',  now()-interval '5 days'),
 ('TXN-100405','CRD-1004','M-QUICKCART',  'PURCHASE','POSTED', 5600.00,'INR',false,null, now()-interval '101 days', now()-interval '100 days'),
 ('TXN-100406','CRD-1004','M-SKYHOP',     'PURCHASE','POSTED', 7800.00,'INR',false,null, now()-interval '81 days',  now()-interval '80 days');

-- Karan's dispute history: three disputes, all lost
INSERT INTO disputes (id, transaction_id, cardholder_id, reason_code, status, queue, disputed_amount, currency,
                      cardholder_statement, duplicate_of_transaction_id, cancellation_date, expected_delivery_date,
                      due_by, created_at, updated_at) VALUES
 ('DSP-9001','TXN-100405','CH-1004','DR-104','RESOLVED_MERCHANT_FAVOUR','DISPUTES',5600.00,'INR',
  'Order never arrived.', null, null, (now()-interval '95 days')::date,
  (now()-interval '45 days')::date, now()-interval '90 days', now()-interval '70 days'),
 ('DSP-9002','TXN-100406','CH-1004','DR-104','RESOLVED_MERCHANT_FAVOUR','DISPUTES',7800.00,'INR',
  'Flight was not provided.', null, null, (now()-interval '79 days')::date,
  (now()-interval '30 days')::date, now()-interval '75 days', now()-interval '50 days'),
 ('DSP-9003','TXN-100402','CH-1004','DR-107','RESOLVED_MERCHANT_FAVOUR','DISPUTES',2500.00,'INR',
  'I cancelled this membership.', null, (now()-interval '70 days')::date, null,
  (now()-interval '15 days')::date, now()-interval '60 days', now()-interval '20 days');

INSERT INTO dispute_events (dispute_id, event_type, detail, actor, occurred_at) VALUES
 ('DSP-9001','CREATED',  'Filed via web form',                                    'cardholder:CH-1004', now()-interval '90 days'),
 ('DSP-9001','RESOLVED', 'MERCHANT_FAVOUR: merchant produced signed delivery proof', 'analyst:a.shah',  now()-interval '70 days'),
 ('DSP-9002','CREATED',  'Filed via web form',                                    'cardholder:CH-1004', now()-interval '75 days'),
 ('DSP-9002','RESOLVED', 'MERCHANT_FAVOUR: boarding pass scanned at gate',         'analyst:r.nair',   now()-interval '50 days'),
 ('DSP-9003','CREATED',  'Filed via web form',                                    'cardholder:CH-1004', now()-interval '60 days'),
 ('DSP-9003','RESOLVED', 'MERCHANT_FAVOUR: gym check-in logs after claimed cancellation', 'analyst:a.shah', now()-interval '20 days');
