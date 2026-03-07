-- Mock NGO data for Nellore, Andhra Pradesh
-- These NGOs are APPROVED and profile_complete so they appear in discovery.

INSERT INTO ngos (
    name,
    address,
    contact_email,
    contact_phone,
    description,
    category_of_work,
    status,
    profile_complete,
    lat,
    lng,
    trust_score,
    trust_tier,
    created_at
) VALUES (
    'Nellore Food Relief Trust',
    'Ramakrishna Nagar, Nellore, Andhra Pradesh, India',
    'contact@nellorefoodrelief.org',
    '+91-98660-11111',
    'Community-driven trust in Nellore focusing on cooked meal distribution, dry ration kits, and emergency food support for low-income families and migrant workers across urban slums and nearby villages.',
    'FOOD',
    'APPROVED',
    TRUE,
    14.4426,
    79.9865,
    72,
    'ESTABLISHED',
    CURRENT_TIMESTAMP
);

INSERT INTO ngos (
    name,
    address,
    contact_email,
    contact_phone,
    description,
    category_of_work,
    status,
    profile_complete,
    lat,
    lng,
    trust_score,
    trust_tier,
    created_at
) VALUES (
    'Coastal Care Foundation Nellore',
    'Magunta Layout, Nellore, Andhra Pradesh, India',
    'hello@coastalcare-nellore.in',
    '+91-98490-22222',
    'Grassroots NGO supporting coastal villages around Nellore with medical camps, chronic medicine support, and health awareness programs for fishing communities and daily wage workers.',
    'MEDICINE',
    'APPROVED',
    TRUE,
    14.4480,
    79.9880,
    80,
    'TRUSTED',
    CURRENT_TIMESTAMP
);

INSERT INTO ngos (
    name,
    address,
    contact_email,
    contact_phone,
    description,
    category_of_work,
    status,
    profile_complete,
    lat,
    lng,
    trust_score,
    trust_tier,
    created_at
) VALUES (
    'Nellore Education & Shelter Society',
    'Pogathota, Nellore, Andhra Pradesh, India',
    'support@ness-nellore.org',
    '+91-97000-33333',
    'Local organisation providing after-school learning support, basic supplies, and temporary shelter assistance for children from vulnerable families in and around Nellore town.',
    'EDUCATION',
    'APPROVED',
    TRUE,
    14.4365,
    79.9820,
    65,
    'ESTABLISHED',
    CURRENT_TIMESTAMP
);

