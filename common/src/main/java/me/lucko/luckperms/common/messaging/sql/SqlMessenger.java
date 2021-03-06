/*
 * This file is part of LuckPerms, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package me.lucko.luckperms.common.messaging.sql;

import me.lucko.luckperms.api.messenger.IncomingMessageConsumer;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.plugin.SchedulerAdapter;
import me.lucko.luckperms.common.plugin.SchedulerTask;
import me.lucko.luckperms.common.storage.dao.sql.SqlDao;

import java.sql.Connection;
import java.sql.SQLException;

public class SqlMessenger extends AbstractSqlMessenger {
    private final LuckPermsPlugin plugin;
    private final SqlDao sqlDao;

    private SchedulerTask pollTask;
    private SchedulerTask housekeepingTask;

    public SqlMessenger(LuckPermsPlugin plugin, SqlDao sqlDao, IncomingMessageConsumer consumer) {
        super(consumer);
        this.plugin = plugin;
        this.sqlDao = sqlDao;
    }

    @Override
    public void init() {
        try {
            super.init();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        // schedule poll tasks
        SchedulerAdapter scheduler = this.plugin.getBootstrap().getScheduler();
        this.pollTask = scheduler.asyncRepeating(this::pollMessages, 20L);
        this.housekeepingTask = scheduler.asyncRepeating(this::runHousekeeping, 20L * 30);
    }

    @Override
    public void close() {
        SchedulerTask task = this.pollTask;
        if (task != null) {
            task.cancel();
        }
        task = this.housekeepingTask;
        if (task != null) {
            task.cancel();
        }

        this.pollTask = null;
        this.housekeepingTask = null;

        super.close();
    }

    @Override
    protected Connection getConnection() throws SQLException {
        return this.sqlDao.getProvider().getConnection();
    }

    @Override
    protected String getTableName() {
        return this.sqlDao.getPrefix().apply("{prefix}messages");
    }
}
