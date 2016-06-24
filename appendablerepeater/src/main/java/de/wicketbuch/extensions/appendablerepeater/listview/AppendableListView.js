/*
 * Copyright (C) 2016 Carl-Eric Menzel <cmenzel@wicketbuch.de>
 * and possibly other appendablerepeater contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
var AppendableListView = {

	newElement: function (newId, tagName) {
		var newElement = document.createElement(tagName);
		newElement.id = newId;
		newElement.style.display = "none";
		return newElement;
	},

	appendAfter: function (existingId, newId, tagName) {
		var existingElement = document.getElementById(existingId);
		var parentElement = existingElement.parentElement;
		var newElement = AppendableListView.newElement(newId, tagName);
		// IE up to 8 doesn't know lastElementChild :-(
 		if (existingElement != parentElement.children[parentElement.children.length - 1]) { 
			existingElement = existingElement.nextElementSibling;
			AppendableListView.insertBefore(existingElement.id, newId);
		} else {
			parentElement.appendChild(newElement);
		}
	},

	insertBefore: function (existingId, newId) {
		var existingElement = document.getElementById(existingId);
	}
};
