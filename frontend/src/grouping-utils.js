
export const groupBy = (authors, criteria) => {
    switch (criteria) {
        case "firstName":
            return groupByFirstName(authors);
        case "lastName":
            return groupByLastName(authors);
        case "nationality":
            return groupByNationality(authors);
        default:
            return groupByLastName(authors);
    }
};

const groupByFirstName = (authors) => {
    return authors.reduce((acc, author) => {
        const firstLetter = author.firstName.charAt(0).toUpperCase();
        if (!acc[firstLetter]) {
            acc[firstLetter] = [];
        }
        acc[firstLetter].push(author);
        return acc;
    }, {});
};

const groupByLastName = (authors) => {
    return authors.reduce((acc, author) => {
        const firstLetter = author.lastName.charAt(0).toUpperCase();
        if (!acc[firstLetter]) {
            acc[firstLetter] = [];
        }
        acc[firstLetter].push(author);
        return acc;
    }, {});
};

const groupByNationality = (authors) => {
    return authors.reduce((acc, author) => {
        const nationality = author.nationality || "Unknown";
        if (!acc[nationality]) {
            acc[nationality] = [];
        }
        acc[nationality].push(author);
        return acc;
    }, {});
};
