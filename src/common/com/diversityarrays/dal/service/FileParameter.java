/*
 * dalserver-interop library - implementation of DAL server for interoperability
 * Copyright (C) 2015  Diversity Arrays Technology
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.diversityarrays.dal.service;

import java.io.File;

/**
 * Provides an implementation of Parameter for java.io.File.
 * @author brian
 *
 */
public class FileParameter extends Parameter<File> {

	public FileParameter(String name, String desc, boolean reqd) {
		super(name, File.class, desc, reqd);
	}

	@Override
	public File stringToValue(String input) throws ParameterException {
		return input == null ? null : new File(input);
	}

	@Override
	public String valueToString(File input) {
		return input==null ? null : input.toString();
	}

}
