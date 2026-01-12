CREATE TABLE payment_order (
    id BIGSERIAL PRIMARY KEY,
    razorpay_order_id VARCHAR(255) NOT NULL UNIQUE,
    amount BIGINT NOT NULL,
    currency VARCHAR(10) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payment_order_razorpay_order_id ON payment_order(razorpay_order_id);
CREATE INDEX idx_payment_order_status ON payment_order(status);

