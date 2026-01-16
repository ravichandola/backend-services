-- ============================================
-- V9: Create payment_transaction table
-- ============================================
-- Payment transactions for Razorpay integration

CREATE TABLE payment_transaction (
    id BIGSERIAL PRIMARY KEY,
    razorpay_payment_id VARCHAR(255),
    razorpay_order_id VARCHAR(255),
    razorpay_signature VARCHAR(255),
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payment_transaction_razorpay_payment_id ON payment_transaction(razorpay_payment_id);
CREATE INDEX idx_payment_transaction_razorpay_order_id ON payment_transaction(razorpay_order_id);
CREATE INDEX idx_payment_transaction_status ON payment_transaction(status);

COMMENT ON TABLE payment_transaction IS 'Payment transactions verified via Razorpay';
COMMENT ON COLUMN payment_transaction.razorpay_payment_id IS 'Razorpay payment identifier';
