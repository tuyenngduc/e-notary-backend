CREATE TABLE IF NOT EXISTS notary_service_types (
    id UUID PRIMARY KEY,
    service_code VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    base_price DECIMAL(15, 2) NOT NULL DEFAULT 0.0,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Insert initial data based on ContractType enum
INSERT INTO notary_service_types (id, service_code, name, base_price, description, is_active)
VALUES 
    (gen_random_uuid(), 'TRANSFER_OF_PROPERTY', 'Hợp đồng chuyển nhượng quyền sở hữu tài sản', 500000, 'Bao gồm mua bán nhà, đất, xe ô tô', TRUE),
    (gen_random_uuid(), 'POWER_OF_ATTORNEY', 'Hợp đồng ủy quyền', 200000, 'Ủy quyền thực hiện các giao dịch dân sự', TRUE),
    (gen_random_uuid(), 'LOAN_AGREEMENT', 'Hợp đồng vay tiền', 300000, 'Hợp đồng vay mượn tiền có tính lãi suất', TRUE),
    (gen_random_uuid(), 'WILL', 'Di chúc', 1000000, 'Lập di chúc phân chia tài sản', TRUE),
    (gen_random_uuid(), 'MARRIAGE_CONTRACT', 'Thỏa thuận tài sản vợ chồng', 800000, 'Xác định tài sản chung, tài sản riêng', TRUE),
    (gen_random_uuid(), 'BUSINESS_CONTRACT', 'Hợp đồng kinh tế', 1500000, 'Hợp đồng giữa các doanh nghiệp', TRUE),
    (gen_random_uuid(), 'OTHER', 'Khác', 100000, 'Các loại hợp đồng, văn bản khác', TRUE);
