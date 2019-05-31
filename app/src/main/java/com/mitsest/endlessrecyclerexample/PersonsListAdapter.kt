package com.mitsest.endlessrecyclerexample

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PersonsListAdapter : RecyclerView.Adapter<PersonsListAdapter.PersonViewHolder>() {

    private var personsList = listOf<Person>()

    override fun getItemCount() = personsList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PersonViewHolder {
        return PersonViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.row_person, parent, false))
    }

    override fun onBindViewHolder(holder: PersonViewHolder, position: Int) {
        try {
            holder.bind(personsList[position])
        } catch (e: IndexOutOfBoundsException) {
            //
        }
    }

    fun addPersons(newPersons: List<Person>) {
        personsList = personsList + newPersons
        notifyDataSetChanged()
    }

    class PersonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val firstNameTextView = itemView.findViewById<TextView>(R.id.first_name)
        private val lastNameTextView = itemView.findViewById<TextView>(R.id.last_name)
        private val favoriteNumberTextView = itemView.findViewById<TextView>(R.id.favorite_number)

        @SuppressLint("SetTextI18n")
        fun bind(person: Person) {
            firstNameTextView.text = "First name: " + person.firstName
            lastNameTextView.text = "Last name: " + person.lastName
            favoriteNumberTextView.text = "Favorite number: " + person.favoriteNumber
        }
    }
}