-- 사용자 테이블
CREATE TABLE IF NOT EXISTS users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username        VARCHAR(50) NOT NULL UNIQUE,
    display_name    VARCHAR(100) NOT NULL,
    avatar_url      VARCHAR(500),
    status          VARCHAR(20) DEFAULT 'OFFLINE',
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 인덱스: 사용자명 검색
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);

-- 채팅방 테이블
CREATE TABLE IF NOT EXISTS chat_rooms (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(100) NOT NULL,
    description     VARCHAR(500),
    type            VARCHAR(20) DEFAULT 'GROUP',
    created_by      UUID NOT NULL REFERENCES users(id),
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 채팅방 멤버 테이블
CREATE TABLE IF NOT EXISTS room_members (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id         UUID NOT NULL REFERENCES chat_rooms(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role            VARCHAR(20) DEFAULT 'MEMBER',
    joined_at       TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    last_read_at    TIMESTAMP WITH TIME ZONE,
    UNIQUE(room_id, user_id)
);

-- 인덱스: 사용자의 채팅방 목록 조회
CREATE INDEX IF NOT EXISTS idx_room_members_user_id ON room_members(user_id);
-- 인덱스: 채팅방의 멤버 목록 조회
CREATE INDEX IF NOT EXISTS idx_room_members_room_id ON room_members(room_id);

-- 메시지 테이블
CREATE TABLE IF NOT EXISTS messages (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id         UUID NOT NULL REFERENCES chat_rooms(id) ON DELETE CASCADE,
    sender_id       UUID NOT NULL REFERENCES users(id),
    content         TEXT NOT NULL,
    message_type    VARCHAR(20) DEFAULT 'TEXT',
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    deleted_at      TIMESTAMP WITH TIME ZONE
);

-- 인덱스: 채팅방별 메시지 조회 (최신순)
CREATE INDEX IF NOT EXISTS idx_messages_room_id_created_at ON messages(room_id, created_at DESC);
-- 인덱스: 발신자별 메시지 조회
CREATE INDEX IF NOT EXISTS idx_messages_sender_id ON messages(sender_id);
