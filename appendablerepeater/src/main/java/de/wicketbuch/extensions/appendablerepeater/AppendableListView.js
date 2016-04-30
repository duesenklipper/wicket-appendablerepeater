var AppendableListView = {
	newElementFrom: function (existingElement, newId) {
		var tagName = existingElement.name;
		var newElement = document.createElement(tagName);
		newElement.id = newId;
		return newElement;
	},

	appendAfter: function (existingId, newId) {
		var existingElement = document.getElementById(existingId);
		var parentElement = existingElement.parentElement;
		if (existingElement != parentElement.lastElementChild) {
			existingElement = existingElement.nextElementSibling;
			AppendableListView.insertBefore(existingElement.id, newId);
		} else {
			var newElement = this.newElementFrom(existingElement, newId);
			parentElement.appendChild(newElement);
		}
	},

	insertBefore: function (existingId, newId) {
		var existingElement = document.getElementById(existingId);
	}
};
