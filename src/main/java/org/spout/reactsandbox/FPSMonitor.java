/*
 * This file is part of ReactSandbox.
 *
 * Copyright (c) 2013 Spout LLC <http://www.spout.org/>
 * ReactSandbox is licensed under the Spout License Version 1.
 *
 * ReactSandbox is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * In addition, 180 days after any changes are published, you can use the
 * software, incorporating those changes, under the terms of the MIT license,
 * as described in the Spout License Version 1.
 *
 * ReactSandbox is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details.
 *
 * You should have received a copy of the GNU Lesser General Public License,
 * the MIT license and the Spout License Version 1 along with this program.
 * If not, see <http://www.gnu.org/licenses/> for the GNU Lesser General Public
 * License and see <http://spout.in/licensev1> for the full license, including
 * the MIT license.
 */
package org.spout.reactsandbox;

/**
 * A basic FPS monitor.
 */
public class FPSMonitor {
    private long lastUpdateTime;
    private long elapsedTime = 0;
    private int frameCount = 0;
    private int framesPerSecond;

    /**
     * Starts the FPS monitor.
     */
    public void start() {
        lastUpdateTime = System.currentTimeMillis();
    }

    /**
     * Updates the frames per seconds.
     */
    public void update() {
        final long time = System.currentTimeMillis();
        elapsedTime += time - lastUpdateTime;
        lastUpdateTime = time;
        frameCount++;
        if (elapsedTime >= 1000) {
            framesPerSecond = frameCount;
            frameCount = 0;
            elapsedTime = 0;
        }
    }

    /**
     * Returns the FPS.
     *
     * @return The FPS
     */
    public int getFPS() {
        return framesPerSecond;
    }
}
