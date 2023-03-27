CREATE TABLE IF NOT EXISTS server (
    server_id INT PRIMARY KEY,
    inactive_days INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS role (
    role_id INT NOT NULL,
    server_id INT NOT NULL,
    add_on_join INT DEFAULT FALSE,
    add_on_inactive INT DEFAULT FALSE,
    PRIMARY KEY (role_id, server_id),
    FOREIGN KEY (server_id) REFERENCES server(server_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS role_requestable (
    role_id INT NOT NULL,
    server_id INT NOT NULL,
    temp INT DEFAULT FALSE,
    agreement TEXT,
    PRIMARY KEY (role_id, server_id),
    FOREIGN KEY (role_id) REFERENCES role(role_id) ON DELETE CASCADE,
    FOREIGN KEY (server_id) REFERENCES role(server_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS role_add_when_removed (
    role_id INT NOT NULL,
    server_id INT NOT NULL,
    to_add_id INT NOT NULL,
    PRIMARY KEY (role_id, server_id, to_add_id),
    FOREIGN KEY (role_id) REFERENCES role(role_id) ON DELETE CASCADE,
    FOREIGN KEY (server_id) REFERENCES role(server_id) ON DELETE CASCADE,
    FOREIGN KEY (to_add_id) REFERENCES role(role_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS role_remove_when_added (
    role_id INT NOT NULL,
    server_id INT NOT NULL,
    to_remove_id INT NOT NULL,
    PRIMARY KEY (role_id, server_id, to_remove_id),
    FOREIGN KEY (role_id) REFERENCES role(role_id) ON DELETE CASCADE,
    FOREIGN KEY (server_id) REFERENCES role(server_id) ON DELETE CASCADE,
    FOREIGN KEY (to_remove_id) REFERENCES role(role_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS user (
    user_id INT PRIMARY KEY,
    salt_amount INT DEFAULT 0,
    salt_last_claim INT DEFAULT 0,
    salt_remind INT DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS member (
    user_id INT NOT NULL,
    server_id INT NOT NULL,
    last_msg INT NOT NULL,
    PRIMARY KEY (user_id, server_id),
    FOREIGN KEY (user_id) REFERENCES user(user_id) ON DELETE CASCADE,
    FOREIGN KEY (server_id) REFERENCES server(server_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS server_msg_log (
    server_id INT NOT NULL,
    channel_id INT NOT NULL,
    PRIMARY KEY (channel_id, server_id),
    FOREIGN KEY (server_id) REFERENCES server(server_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS server_usr_log (
    server_id INT NOT NULL,
    channel_id INT NOT NULL,
    PRIMARY KEY (channel_id, server_id),
    FOREIGN KEY (server_id) REFERENCES server(server_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS timer (
    timer_id INTEGER PRIMARY KEY AUTOINCREMENT,
    channel_id INT NOT NULL,
    user_id INT NOT NULL,
    message TEXT NOT NULL,
    set_on TIME NOT NULL,
    end_on TIME NOT NULL
);

CREATE TABLE IF NOT EXISTS role_timer (
    timer_id INTEGER PRIMARY KEY AUTOINCREMENT,
    role_id INT NOT NULL,
    server_id INT NOT NULL,
    user_id INT NOT NULL,
    end_on TIME NOT NULL,
    FOREIGN KEY (role_id) REFERENCES role(role_id) ON DELETE CASCADE,
    FOREIGN KEY (server_id) REFERENCES server(server_id) ON DELETE CASCADE
);